package com.emailagent.dto.response.notification;

import com.emailagent.domain.entity.Notification;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NotificationResponse {

    @JsonProperty("notification_id")
    private Long notificationId;

    private String type;
    private String title;

    @JsonProperty("noti_message")
    private String notiMessage;

    @JsonProperty("related_id")
    private Long relatedId;

    @JsonProperty("is_read")
    private boolean isRead;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .type(notification.getType())
                .title(notification.getTitle())
                .notiMessage(notification.getNotiMessage())
                .relatedId(notification.getRelatedId())
                .isRead(notification.isRead())
                .build();
    }

    /** 목록 응답 래퍼 */
    @Getter
    @Builder
    public static class ListResponse extends BaseResponse {
        private List<NotificationResponse> notifications;
    }
}
