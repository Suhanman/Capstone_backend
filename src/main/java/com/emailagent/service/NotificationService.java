package com.emailagent.service;

import com.emailagent.domain.entity.Notification;
import com.emailagent.dto.response.auth.BaseResponse;
import com.emailagent.dto.response.notification.NotificationResponse;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // =============================================
    // GET /api/notifications?is_read=false
    // is_read 파라미터 없으면 전체, false면 미확인만
    // =============================================
    @Transactional(readOnly = true)
    public NotificationResponse.ListResponse getNotifications(Long userId, Boolean isRead) {
        List<NotificationResponse> data;

        if (Boolean.FALSE.equals(isRead)) {
            // is_read=false → 미확인 알림만
            data = notificationRepository
                    .findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                    .stream()
                    .map(NotificationResponse::from)
                    .toList();
        } else {
            // 파라미터 없음 또는 is_read=true → 전체 조회
            data = notificationRepository
                    .findByUser_UserIdOrderByCreatedAtDesc(userId)
                    .stream()
                    .map(NotificationResponse::from)
                    .toList();
        }

        return NotificationResponse.ListResponse.builder()
                .notifications(data)
                .build();
    }

    // =============================================
    // PATCH /api/notifications/{notification_id}/read
    // 단일 알림 읽음 처리 (소유권 검증 필수)
    // =============================================
    @Transactional
    public BaseResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findByNotificationIdAndUser_UserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("알림을 찾을 수 없습니다."));

        notification.markAsRead();

        return new BaseResponse();
    }

    // =============================================
    // PATCH /api/notifications/read-all
    // 전체 미확인 알림 일괄 읽음 처리
    // =============================================
    @Transactional
    public BaseResponse markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
        return new BaseResponse();
    }
}
