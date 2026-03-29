package com.emailagent.service.admin;

import com.emailagent.dto.response.admin.dashboard.AdminDashboardSummaryResponse;
import com.emailagent.dto.response.admin.dashboard.AdminDomainDistributionResponse;
import com.emailagent.dto.response.admin.dashboard.AdminEmailVolumeResponse;
import com.emailagent.dto.response.admin.dashboard.AdminWeeklyTrendResponse;
import com.emailagent.repository.DraftReplyRepository;
import com.emailagent.repository.EmailAnalysisResultRepository;
import com.emailagent.repository.EmailRepository;
import com.emailagent.repository.IntegrationRepository;
import com.emailagent.repository.SupportTicketRepository;
import com.emailagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final UserRepository userRepository;
    private final IntegrationRepository integrationRepository;
    private final EmailAnalysisResultRepository emailAnalysisResultRepository;
    private final DraftReplyRepository draftReplyRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final EmailRepository emailRepository;

    /**
     * 관리자 대시보드 상단 요약 카드 집계
     * - 오늘 기준: 자정(00:00) ~ 내일 자정 범위로 집계
     */
    public AdminDashboardSummaryResponse getSummary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        long totalUsers = userRepository.count();
        // Granular Consent 적용: Gmail(필수) / Calendar(선택) 연동자 수 각각 집계
        long gmailConnectedUsers = integrationRepository.countByIsGmailConnectedTrue();
        long calendarConnectedUsers = integrationRepository.countByIsCalendarConnectedTrue();
        long todayAnalyzedEmails = emailAnalysisResultRepository.countByCreatedAtBetween(todayStart, tomorrowStart);
        long todayGeneratedDrafts = draftReplyRepository.countByCreatedAtBetween(todayStart, tomorrowStart);
        long totalSupportTickets = supportTicketRepository.count();

        return new AdminDashboardSummaryResponse(
                totalUsers,
                gmailConnectedUsers,
                calendarConnectedUsers,
                todayAnalyzedEmails,
                todayGeneratedDrafts,
                totalSupportTickets
        );
    }

    /**
     * 기간별 메일 처리량 통계 조회
     * - start_date ~ end_date 범위의 날짜별 수신 메일 수 반환
     * - 조회 결과에 없는 날짜(0건)는 포함하지 않음 (DB 없는 날짜는 자연히 제외)
     */
    public AdminEmailVolumeResponse getEmailVolume(String startDate, String endDate) {
        LocalDateTime start = LocalDate.parse(startDate, DATE_FORMATTER).atStartOfDay();
        // end_date 당일을 포함하기 위해 다음 날 자정까지 조회
        LocalDateTime end = LocalDate.parse(endDate, DATE_FORMATTER).plusDays(1).atStartOfDay();

        List<Object[]> rows = emailRepository.countByDateRangeGroupedByDate(start, end);
        List<AdminEmailVolumeResponse.VolumeEntry> volumeData = rows.stream()
                .map(row -> new AdminEmailVolumeResponse.VolumeEntry(
                        row[0].toString(),
                        ((Number) row[1]).longValue()
                ))
                .collect(Collectors.toList());

        return new AdminEmailVolumeResponse(volumeData);
    }

    /**
     * 도메인별 이메일 분포 통계 조회
     * - sender_email 기준 도메인 추출, 상위 N개 반환
     */
    public AdminDomainDistributionResponse getDomainDistribution(int limit) {
        List<Object[]> rows = emailRepository.findTopDomains(limit);
        List<AdminDomainDistributionResponse.DomainEntry> domainData = rows.stream()
                .map(row -> new AdminDomainDistributionResponse.DomainEntry(
                        (String) row[0],
                        ((Number) row[1]).longValue()
                ))
                .collect(Collectors.toList());

        return new AdminDomainDistributionResponse(domainData);
    }

    /**
     * 최근 7일 처리 추이 조회
     * - 오늘 포함 7일치 날짜별 수신 메일 수 + 초안 생성 수
     * - DB에 데이터 없는 날짜는 0으로 채워 반환 (프론트 차트 연속성 보장)
     */
    public AdminWeeklyTrendResponse getWeeklyTrend() {
        // 7일 전 자정부터 조회
        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(6).atStartOfDay();

        // 날짜별 수신 메일 수 맵 생성
        Map<String, Long> receivedMap = emailRepository.countReceivedGroupedByDate(sevenDaysAgo)
                .stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> ((Number) row[1]).longValue()
                ));

        // 날짜별 초안 생성 수 맵 생성
        Map<String, Long> draftMap = draftReplyRepository.countDraftsGroupedByDate(sevenDaysAgo)
                .stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> ((Number) row[1]).longValue()
                ));

        // 최근 7일 날짜 순서대로 0 포함하여 빌드
        List<AdminWeeklyTrendResponse.TrendEntry> trendData = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).format(DATE_FORMATTER);
            long receivedCount = receivedMap.getOrDefault(date, 0L);
            long draftCount = draftMap.getOrDefault(date, 0L);
            trendData.add(new AdminWeeklyTrendResponse.TrendEntry(date, receivedCount, draftCount));
        }

        return new AdminWeeklyTrendResponse(trendData);
    }
}
