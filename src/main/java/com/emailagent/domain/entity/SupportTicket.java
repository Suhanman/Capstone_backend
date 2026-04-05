package com.emailagent.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long ticketId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // PENDING(대기) / ANSWERED(완료)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "admin_reply", columnDefinition = "TEXT")
    private String adminReply;

    // 답변 작성 관리자 ID (Users.user_id 참조, DB FK 존재)
    @Column(name = "replied_by")
    private Long repliedBy;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 관리자 답변 작성: 상태를 ANSWERED로 변경하고 답변 정보 기록
    public void registerAdminReply(String reply, Long adminUserId) {
        this.adminReply = reply;
        this.repliedBy = adminUserId;
        this.repliedAt = LocalDateTime.now();
        this.status = "ANSWERED";
    }
}
