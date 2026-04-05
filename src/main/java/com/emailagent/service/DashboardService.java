package com.emailagent.service;

import com.emailagent.domain.entity.CalendarEvent;
import com.emailagent.domain.entity.Email;
import com.emailagent.domain.entity.EmailAnalysisResult;
import com.emailagent.domain.entity.Integration;
import com.emailagent.domain.enums.DraftStatus;
import com.emailagent.dto.response.dashboard.*;
import com.emailagent.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EmailRepository emailRepository;
    private final EmailAnalysisResultRepository analysisResultRepository;
    private final DraftReplyRepository draftReplyRepository;
    private final IntegrationRepository integrationRepository;
    private final CalendarEventRepository calendarEventRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // =============================================
    // GET /api/dashboard/summary
    // =============================================

    @Transactional(readOnly = true)
    public SummaryResponse getSummary(Long userId) {
        LocalDate today = LocalDate.now();

        // 오늘/어제 수신 이메일 수
        long todayCount    = countEmails(userId, today, today.plusDays(1));
        long yesterdayCount = countEmails(userId, today.minusDays(1), today);

        // 검토 대기 초안 수
        long pendingDrafts = draftReplyRepository.countByUser_UserIdAndStatus(userId, DraftStatus.PENDING_REVIEW);

        // 이번 주 / 지난 주 템플릿 매칭률
        LocalDate monday      = today.with(DayOfWeek.MONDAY);
        LocalDate nextMonday  = monday.plusWeeks(1);
        LocalDate lastMonday  = monday.minusWeeks(1);

        double thisWeekRate = calcMatchingRate(userId, monday, nextMonday);
        double lastWeekRate = calcMatchingRate(userId, lastMonday, monday);
        double rateDiff     = round1(thisWeekRate - lastWeekRate);

        // 연동 상태
        Integration integration = integrationRepository.findByUser_UserId(userId).orElse(null);

        return SummaryResponse.builder()
                .processedToday(SummaryResponse.ProcessedToday.builder()
                        .count(todayCount)
                        .diffFromYesterday(todayCount - yesterdayCount)
                        .build())
                .pendingDrafts(SummaryResponse.PendingDrafts.builder()
                        .count(pendingDrafts)
                        .build())
                .templateMatching(SummaryResponse.TemplateMatching.builder()
                        .rate(round1(thisWeekRate))
                        .diffFromLastWeek(rateDiff)
                        .build())
                .integrationStatus(SummaryResponse.IntegrationStatus.builder()
                        .status(integration != null ? integration.getSyncStatus().name() : "DISCONNECTED")
                        .connectedEmail(integration != null ? integration.getConnectedEmail() : null)
                        .build())
                .build();
    }

    // =============================================
    // GET /api/dashboard/schedules
    // =============================================

    @Transactional(readOnly = true)
    public ScheduleResponse getSchedules(Long userId) {
        List<CalendarEvent> events = calendarEventRepository.findUpcoming(userId, LocalDateTime.now());

        List<ScheduleResponse.ScheduleItem> items = events.stream()
                .limit(5)
                .map(ScheduleResponse.ScheduleItem::from)
                .toList();

        return ScheduleResponse.builder()
                .schedules(items)
                .build();
    }

    // =============================================
    // GET /api/dashboard/weekly-summary
    // =============================================

    @Transactional(readOnly = true)
    public WeeklySummaryResponse getWeeklySummary(Long userId) {
        LocalDate monday     = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate nextMonday = monday.plusWeeks(1);

        LocalDateTime start = monday.atStartOfDay();
        LocalDateTime end   = nextMonday.atStartOfDay();

        List<Object[]> rows = analysisResultRepository.findWeeklyCategorySummary(userId, start, end);

        List<WeeklySummaryResponse.CategoryStat> categories = rows.stream()
                .map(row -> WeeklySummaryResponse.CategoryStat.builder()
                        .categoryName((String) row[0])
                        .count((Long) row[1])
                        .color((String) row[2])
                        .build())
                .toList();

        return WeeklySummaryResponse.builder()
                .dateRange(WeeklySummaryResponse.DateRange.builder()
                        .start(monday.format(DATE_FMT))
                        .end(monday.plusDays(6).format(DATE_FMT)) // 일요일
                        .build())
                .categories(categories)
                .build();
    }

    // =============================================
    // GET /api/dashboard/recent-emails
    // =============================================

    @Transactional(readOnly = true)
    public RecentEmailResponse getRecentEmails(Long userId) {
        // 1차 쿼리: 최근 이메일 5건
        List<Email> emails = emailRepository.findRecentByUserId(userId, PageRequest.of(0, 5));

        // 2차 쿼리: 분석 결과 일괄 조회 (N+1 방지)
        List<Long> emailIds = emails.stream().map(Email::getEmailId).toList();
        Map<Long, EmailAnalysisResult> analysisMap = analysisResultRepository
                .findByEmailIdsWithCategory(emailIds)
                .stream()
                .collect(Collectors.toMap(ar -> ar.getEmail().getEmailId(), ar -> ar));

        List<RecentEmailResponse.EmailItem> items = emails.stream()
                .map(email -> {
                    EmailAnalysisResult ar = analysisMap.get(email.getEmailId());
                    String categoryName = (ar != null && ar.getCategory() != null)
                            ? ar.getCategory().getCategoryName() : null;
                    String senderCompany = (ar != null && ar.getEntitiesJson() != null)
                            ? (String) ar.getEntitiesJson().get("company") : null;

                    return RecentEmailResponse.EmailItem.builder()
                            .emailId(email.getEmailId())
                            .senderName(email.getSenderName())
                            .senderCompany(senderCompany)
                            .subject(email.getSubject())
                            .categoryName(categoryName)
                            .status(email.getStatus().name())
                            .receivedAt(email.getReceivedAt())
                            .build();
                })
                .toList();

        return RecentEmailResponse.builder()
                .emails(items)
                .build();
    }

    // =============================================
    // private helpers
    // =============================================

    private long countEmails(Long userId, LocalDate from, LocalDate to) {
        return emailRepository.countByUserIdAndDateRange(userId, from.atStartOfDay(), to.atStartOfDay());
    }

    private double calcMatchingRate(Long userId, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atStartOfDay();
        long total   = emailRepository.countByUserIdAndDateRange(userId, start, end);
        long matched = analysisResultRepository.countMatchedByUserIdAndDateRange(userId, start, end);
        return total > 0 ? (double) matched / total * 100 : 0.0;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
