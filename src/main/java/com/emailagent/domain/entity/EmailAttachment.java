package com.emailagent.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "EmailAttachments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EmailAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long attachmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", nullable = false)
    private Email email;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "mime_type", length = 200)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    /** 로컬 저장 경로 (실제 파일 서빙용) */
    @Column(name = "file_path", length = 1000)
    private String filePath;

    /** Gmail 첨부파일 식별자. GmailApiService.getAttachmentBytes() 호출 시 사용 */
    @Column(name = "external_attachment_id", length = 256)
    private String externalAttachmentId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
