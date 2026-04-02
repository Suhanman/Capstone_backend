package com.emailagent.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "CalendarEvents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id")
    private Email email; // 이메일에서 감지된 경우 연결, nullable

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDatetime;

    @Column(name = "end_datetime")
    private LocalDateTime endDatetime;

    @Column(name = "source", length = 20)
    @Builder.Default
    private String source = "EMAIL"; // EMAIL / MANUAL

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING / CONFIRMED / CANCELLED

    @Column(name = "is_calendar_added", nullable = false)
    @Builder.Default
    private boolean isCalendarAdded = false;

    // Google Calendar API 호출 후 반환받은 이벤트 ID (update/delete 시 사용)
    @Column(name = "google_event_id", length = 255)
    private String googleEventId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updateStatus(String status) {
        this.status = status;
    }

    public void update(String title, LocalDateTime startDatetime, LocalDateTime endDatetime) {
        this.title = title;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
    }

    /** Google Calendar API 등록 완료 후 이벤트 ID 및 상태 반영 */
    public void markAsCalendarAdded(String googleEventId) {
        this.isCalendarAdded = true;
        this.googleEventId = googleEventId;
    }
}
