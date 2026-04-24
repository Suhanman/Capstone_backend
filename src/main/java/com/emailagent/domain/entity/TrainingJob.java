package com.emailagent.domain.entity;

import com.emailagent.domain.enums.TrainingJobStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "training_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TrainingJob {

    @Id
    @Column(name = "job_id", length = 64, nullable = false)
    private String jobId;

    @Column(name = "job_type", length = 50, nullable = false)
    private String jobType;

    @Column(name = "task_type", length = 50, nullable = false)
    private String taskType;

    @Column(name = "dataset_version", length = 50)
    private String datasetVersion;

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TrainingJobStatus status = TrainingJobStatus.QUEUED;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Column(name = "metrics_json", columnDefinition = "LONGTEXT")
    private String metricsJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    /** AI worker 완료 이벤트 수신 시 COMPLETED 처리 */
    public void complete(String modelVersion, String metricsJson, LocalDateTime finishedAt) {
        this.status = TrainingJobStatus.COMPLETED;
        this.modelVersion = modelVersion;
        this.metricsJson = metricsJson;
        this.finishedAt = finishedAt;
    }

    /** AI worker 실패 이벤트 수신 시 FAILED 처리 */
    public void fail(String errorMessage, LocalDateTime finishedAt) {
        this.status = TrainingJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.finishedAt = finishedAt;
    }
}
