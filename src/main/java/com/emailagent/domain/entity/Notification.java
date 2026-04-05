package com.emailagent.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 알림 유형 (예: EMAIL_RECEIVED, DRAFT_READY, CALENDAR_ADDED 등)
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "noti_message", columnDefinition = "TEXT")
    private String notiMessage;

    // 연관 리소스 ID (이메일 ID, 일정 ID 등 nullable)
    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── 비즈니스 메서드 ──────────────────────────────────────────────────────────

    public void markAsRead() {
        this.isRead = true;
    }
}
