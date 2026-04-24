package com.emailagent.domain.entity;

import com.emailagent.domain.enums.ModelStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "trained_models",
    uniqueConstraints = @UniqueConstraint(name = "uq_trained_model_version", columnNames = "model_version")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TrainedModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "model_id")
    private Long modelId;

    // 모델 버전 식별자 (예: v2026_04_12_045539)
    @Column(name = "model_version", length = 100, nullable = false)
    private String modelVersion;

    // training_jobs.job_id 참조 (nullable — job 없이도 모델 존재 가능)
    @Column(name = "job_id", length = 64)
    private String jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ModelStatus status = ModelStatus.INACTIVE;

    // metrics에서 추출한 핵심 지표 (목록 조회 성능 최적화용)
    @Column(name = "intent_f1")
    private Double intentF1;

    @Column(name = "domain_accuracy")
    private Double domainAccuracy;

    @Column(name = "metrics_json", columnDefinition = "LONGTEXT")
    private String metricsJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 이 모델을 운영용(ACTIVE)으로 전환 */
    public void activate() {
        this.status = ModelStatus.ACTIVE;
    }

    /** 이 모델을 비활성(INACTIVE)으로 전환 */
    public void deactivate() {
        this.status = ModelStatus.INACTIVE;
    }
}
