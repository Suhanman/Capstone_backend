package com.emailagent.domain.entity;

import com.emailagent.domain.enums.EmailStatus;
import com.emailagent.domain.enums.ImportanceLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Emails")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_id")
    private Long emailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "external_msg_id", nullable = false, unique = true, length = 128)
    private String externalMsgId;

    @Column(name = "sender_name", length = 100)
    private String senderName;

    @Column(name = "sender_email", nullable = false, length = 255)
    private String senderEmail;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "body_raw", columnDefinition = "LONGTEXT")
    private String bodyRaw;

    @Column(name = "body_clean", nullable = false, columnDefinition = "LONGTEXT")
    private String bodyClean;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private EmailStatus status = EmailStatus.PENDING_REVIEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "importance_level", nullable = false)
    @Builder.Default
    private ImportanceLevel importanceLevel = ImportanceLevel.MEDIUM;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 상태 변경 메서드
    public void updateStatus(EmailStatus status) {
        this.status = status;
    }

    public void updateImportance(ImportanceLevel level) {
        this.importanceLevel = level;
    }
}
