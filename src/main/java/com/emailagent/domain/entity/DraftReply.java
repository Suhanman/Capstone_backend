package com.emailagent.domain.entity;

import com.emailagent.converter.VectorConverter;
import com.emailagent.domain.enums.DraftStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "DraftReplies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DraftReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "draft_reply_id")
    private Long draftReplyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", nullable = false, unique = true)
    private Email email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template; // AI가 선택한 템플릿, nullable

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private DraftStatus status = DraftStatus.PENDING_REVIEW;

    @Column(name = "draft_subject", length = 500)
    private String draftSubject;

    @Column(name = "draft_content", columnDefinition = "TEXT")
    private String draftContent;

    // AI가 생성한 답장 임베딩 벡터 (MariaDB VECTOR(384) 바이너리)
    @Column(name = "reply_embedding")
    @Convert(converter = VectorConverter.class)
    private float[] replyEmbedding;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updateStatus(DraftStatus status) {
        this.status = status;
    }

    /**
     * draft 큐 AI 결과 수신 후 내용 갱신
     * replyEmbedding: float[] (MariaDB VECTOR(384) 바이너리로 저장)
     */
    public void updateContent(String draftSubject, String draftContent, float[] replyEmbedding) {
        this.draftSubject = draftSubject;
        this.draftContent = draftContent;
        this.replyEmbedding = replyEmbedding;
    }
}
