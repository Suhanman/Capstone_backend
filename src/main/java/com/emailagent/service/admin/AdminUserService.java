package com.emailagent.service.admin;

import com.emailagent.domain.entity.BusinessProfile;
import com.emailagent.domain.entity.Integration;
import com.emailagent.domain.entity.User;
import com.emailagent.dto.response.admin.user.AdminDeleteIntegrationResponse;
import com.emailagent.dto.response.admin.user.AdminUserDetailResponse;
import com.emailagent.dto.response.admin.user.AdminUserIntegrationResponse;
import com.emailagent.dto.response.admin.user.AdminUserListResponse;
import com.emailagent.dto.response.admin.user.AdminUserStatusUpdateResponse;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.repository.BusinessProfileRepository;
import com.emailagent.repository.DraftReplyRepository;
import com.emailagent.repository.EmailRepository;
import com.emailagent.repository.IntegrationRepository;
import com.emailagent.repository.SupportTicketRepository;
import com.emailagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final EmailRepository emailRepository;
    private final DraftReplyRepository draftReplyRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final IntegrationRepository integrationRepository;

    /**
     * 전체 사용자 목록 조회 (이름/이메일/업종 검색 + 페이징)
     * - search_type: "name" | "email" | "industry_type"
     * - search_keyword: 검색어 (빈 문자열이면 전체 조회)
     */
    @Transactional(readOnly = true)
    public AdminUserListResponse getUsers(String searchType, String searchKeyword, int page, int size) {
        // page는 0-based로 변환
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<User> userPage;
        boolean hasKeyword = searchKeyword != null && !searchKeyword.isBlank();

        if (hasKeyword && "industry_type".equals(searchType)) {
            userPage = userRepository.findByIndustryTypeKeyword(searchKeyword, pageable);
        } else if (hasKeyword && "email".equals(searchType)) {
            userPage = userRepository.findByEmailContainingIgnoreCase(searchKeyword, pageable);
        } else if (hasKeyword && "name".equals(searchType)) {
            userPage = userRepository.findByNameContainingIgnoreCase(searchKeyword, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        // 페이지 내 사용자 ID로 BusinessProfile 배치 조회 (N+1 방지)
        List<Long> userIds = userPage.getContent().stream()
                .map(User::getUserId)
                .collect(Collectors.toList());

        Map<Long, String> industryTypeMap = businessProfileRepository.findAllByUserIds(userIds)
                .stream()
                .collect(Collectors.toMap(
                        bp -> bp.getUser().getUserId(),
                        bp -> bp.getIndustryType() != null ? bp.getIndustryType() : ""
                ));

        List<AdminUserListResponse.UserItem> userItems = userPage.getContent().stream()
                .map(u -> new AdminUserListResponse.UserItem(
                        u.getUserId(),
                        u.getEmail(),
                        u.getName(),
                        industryTypeMap.getOrDefault(u.getUserId(), null),
                        u.isActive(),
                        u.getCreatedAt().toInstant(ZoneOffset.UTC).toString()
                ))
                .collect(Collectors.toList());

        return new AdminUserListResponse(userPage.getTotalElements(), userItems);
    }

    /**
     * 특정 사용자 상세 정보 조회
     * - Users + BusinessProfiles + 집계 수치
     */
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. userId=" + userId));

        BusinessProfile profile = businessProfileRepository.findByUser_UserId(userId).orElse(null);

        long totalProcessedEmails = emailRepository.countProcessedByUserId(userId);
        long totalGeneratedDrafts = draftReplyRepository.countByUser_UserId(userId);
        long recentTicketCount = supportTicketRepository.countByUser_UserId(userId);

        // last_login_at: null이면 빈 문자열 대신 null 반환 (프론트에서 처리)
        String lastLoginAt = user.getLastLoginAt() != null
                ? user.getLastLoginAt().toInstant(ZoneOffset.UTC).toString()
                : null;

        return new AdminUserDetailResponse(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.isActive(),
                lastLoginAt,
                profile != null ? profile.getIndustryType() : null,
                profile != null && profile.getEmailTone() != null ? profile.getEmailTone().name() : null,
                profile != null ? profile.getCompanyDescription() : null,
                totalProcessedEmails,
                totalGeneratedDrafts,
                recentTicketCount
        );
    }

    /**
     * 특정 사용자 계정 활성/비활성 상태 변경
     */
    @Transactional
    public AdminUserStatusUpdateResponse updateUserStatus(Long userId, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. userId=" + userId));

        user.updateActiveStatus(isActive);

        return new AdminUserStatusUpdateResponse(user.getUserId(), user.isActive());
    }

    /**
     * 특정 사용자의 Gmail / Calendar 연동 상태 조회
     * - granted_scopes 문자열에서 gmail/calendar scope 포함 여부로 연동 여부 판별
     * - 날짜 포맷: "yyyy-MM-dd HH:mm:ss"
     */
    @Transactional(readOnly = true)
    public AdminUserIntegrationResponse getUserIntegration(Long userId) {
        Integration integration = integrationRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 사용자의 Google 연동 정보를 찾을 수 없습니다. userId=" + userId));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String scopes = integration.getGrantedScopes() != null ? integration.getGrantedScopes().toLowerCase() : "";
        boolean gmailConnected = scopes.contains("gmail") || scopes.contains("mail.google");
        boolean calendarConnected = scopes.contains("calendar");

        String integratedAt = integration.getCreatedAt().atOffset(ZoneOffset.UTC)
                .toLocalDateTime().format(formatter);
        String lastSyncAt = integration.getLastSyncedAt() != null
                ? integration.getLastSyncedAt().atOffset(ZoneOffset.UTC).toLocalDateTime().format(formatter)
                : null;

        return new AdminUserIntegrationResponse(
                gmailConnected,
                calendarConnected,
                integration.getConnectedEmail(),
                integratedAt,
                lastSyncAt
        );
    }

    /**
     * 특정 사용자의 Google 연동 강제 해제
     * - Integration 레코드 삭제 (ON DELETE CASCADE로 관련 데이터도 처리됨)
     */
    @Transactional
    public AdminDeleteIntegrationResponse deleteUserIntegration(Long userId) {
        // 존재 여부 확인 (없으면 이미 해제 상태이므로 예외 발생)
        if (!integrationRepository.existsByUser_UserId(userId)) {
            throw new ResourceNotFoundException("해당 사용자의 Google 연동 정보를 찾을 수 없습니다. userId=" + userId);
        }
        integrationRepository.deleteByUser_UserId(userId);
        return new AdminDeleteIntegrationResponse();
    }
}
