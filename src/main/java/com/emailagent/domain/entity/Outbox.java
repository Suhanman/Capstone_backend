package com.emailagent.domain.entity;

import com.emailagent.domain.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "outbox",
       indexes = {
           @Index(name = "idx_outbox_status", columnList = "status"),
           @Index(name = "idx_outbox_email_id", columnList = "email_id"),
           @Index(name = "idx_outbox_status_created", columnList = "status, created_at"),
           @Index(name = "idx_outbox_sent_at", columnList = "sent_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_id")
    private Long outboxId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", nullable = false)
    private Email email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.READY;

    // AI 서버로 전달할 페이로드 (email_id, body_clean 등)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private Map<String, Object> payload;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    // 상태 전이 메서드
    public void markAsSending() {
        this.status = OutboxStatus.SENDING;
        this.sentAt = LocalDateTime.now();
    }

    public void markAsFinished() {
        this.status = OutboxStatus.FINISH;
        this.finishedAt = LocalDateTime.now();
    }

    /** 발행 재시도 카운트 증가 (retry 판단은 Service 계층에서 수행) */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /** 단순 READY 롤백 — 타임아웃 복구 시 retryCount 소모 없이 재투입 */
    public void markAsReady() {
        this.status = OutboxStatus.READY;
    }

    /** FAILED 확정 — AI 오류 또는 재시도 한도 초과 시 Service에서 판단 후 호출 */
    public void markAsFailed(String reason) {
        this.status = OutboxStatus.FAILED;
        this.failReason = reason;
        this.finishedAt = LocalDateTime.now();
    }

    /**
     * CANCELLED 확정 — 사용자가 Gmail에서 해당 이메일을 삭제한 경우 호출.
     * FAILED(AI 오류)와 원인을 명확히 구분하여 관리자 로그 식별성을 확보한다.
     * fail_reason 컬럼에 "DELETED_BY_USER" 등의 사유를 기록하여 추후 모니터링에 활용.
     */
    public void markAsCancelled(String reason) {
        this.status = OutboxStatus.CANCELLED;
        this.failReason = reason;
        this.finishedAt = LocalDateTime.now();
    }
}
