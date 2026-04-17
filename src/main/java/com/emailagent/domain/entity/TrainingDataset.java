package com.emailagent.domain.entity;

import com.emailagent.domain.enums.DatasetStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "training_datasets",
    uniqueConstraints = @UniqueConstraint(name = "uq_dataset_version", columnNames = "dataset_version")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TrainingDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dataset_id")
    private Long datasetId;

    // 데이터셋 버전 식별자 (예: v20260413_153045)
    @Column(name = "dataset_version", length = 50, nullable = false)
    private String datasetVersion;

    // 수집 시점의 이메일 레코드 수 (학습 데이터 규모 확인용)
    @Column(name = "record_count", nullable = false)
    private int recordCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DatasetStatus status = DatasetStatus.READY;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
