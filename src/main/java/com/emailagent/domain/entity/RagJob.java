package com.emailagent.domain.entity;

import com.emailagent.domain.enums.RagJobStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "rag_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RagJob {

    @Id
    @Column(name = "job_id", nullable = false, length = 120)
    private String jobId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "request_id", length = 120)
    private String requestId;

    @Column(name = "job_type", nullable = false, length = 80)
    private String jobType;

    @Column(name = "target_type", length = 80)
    private String targetType;

    @Column(name = "target_id", length = 120)
    private String targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private RagJobStatus status = RagJobStatus.QUEUED;

    @Column(name = "progress_step", length = 120)
    private String progressStep;

    @Column(name = "progress_message", length = 500)
    private String progressMessage;

    @Column(name = "error_code", length = 120)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public void markQueued(String progressStep, String progressMessage, String payloadJson) {
        this.status = RagJobStatus.QUEUED;
        this.progressStep = progressStep;
        this.progressMessage = progressMessage;
        this.payloadJson = payloadJson;
        this.errorCode = null;
        this.errorMessage = null;
        this.completedAt = null;
    }

    public void markProcessing(String progressStep, String progressMessage, String payloadJson) {
        this.status = RagJobStatus.PROCESSING;
        this.progressStep = progressStep;
        this.progressMessage = progressMessage;
        this.payloadJson = payloadJson;
        this.errorCode = null;
        this.errorMessage = null;
    }

    public void markCompleted(String progressStep, String progressMessage, String payloadJson) {
        this.status = RagJobStatus.COMPLETED;
        this.progressStep = progressStep;
        this.progressMessage = progressMessage;
        this.payloadJson = payloadJson;
        this.errorCode = null;
        this.errorMessage = null;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String progressStep, String progressMessage, String errorCode, String errorMessage, String payloadJson) {
        this.status = RagJobStatus.FAILED;
        this.progressStep = progressStep;
        this.progressMessage = progressMessage;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.payloadJson = payloadJson;
        this.completedAt = LocalDateTime.now();
    }
}
