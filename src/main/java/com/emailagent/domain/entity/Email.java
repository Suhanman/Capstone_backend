package com.emailagent.domain.entity;

import com.emailagent.converter.AttachmentMetaConverter;
import com.emailagent.domain.enums.EmailStatus;
import com.emailagent.domain.enums.ImportanceLevel;
import com.emailagent.dto.inbox.AttachmentMetaDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "emails")
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

    /** 첨부파일 유무 플래그 */
    @Column(name = "has_attachments", nullable = false)
    @Builder.Default
    private boolean hasAttachments = false;

    /** 첨부파일 메타데이터 배열 (attachment_id 시퀀스 → gmail_attachment_id 매핑 포함) */
    @Convert(converter = AttachmentMetaConverter.class)
    @Column(name = "attachments_meta", columnDefinition = "JSON")
    private List<AttachmentMetaDto> attachmentsMeta;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 분석 결과 역방향 매핑 (inbox 조회 FETCH JOIN용, 추가 컬럼 없음)
    @OneToOne(mappedBy = "email", fetch = FetchType.LAZY)
    private EmailAnalysisResult analysisResult;

    // 상태 변경 메서드
    public void updateStatus(EmailStatus status) {
        this.status = status;
    }

    public void updateImportance(ImportanceLevel level) {
        this.importanceLevel = level;
    }

    /** 첨부파일 메타데이터 일괄 세팅 (PubSub 파이프라인에서 Email 저장 후 호출) */
    public void updateAttachmentMeta(boolean hasAttachments, List<AttachmentMetaDto> attachmentsMeta) {
        this.hasAttachments = hasAttachments;
        this.attachmentsMeta = attachmentsMeta;
    }
}
