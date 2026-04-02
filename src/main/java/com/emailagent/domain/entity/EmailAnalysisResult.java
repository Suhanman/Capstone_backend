package com.emailagent.domain.entity;

import com.emailagent.converter.VectorConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "EmailAnalysisResults")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EmailAnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_result_id")
    private Long analysisResultId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", nullable = false, unique = true)
    private Email email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "domain", length = 100)
    private String domain;          // 예: "마케팅"

    @Column(name = "intent", length = 100)
    private String intent;          // 예: "광고 문의"

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "schedule_detected", nullable = false)
    @Builder.Default
    private boolean scheduleDetected = false;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    // JSON 타입으로 저장 (엔티티 추출 결과)
    // 예: {"customer_name":"홍길동","company":"ABC Corp","date":"2026-04-03"}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entities_json", columnDefinition = "JSON")
    private Map<String, Object> entitiesJson;

    // AI가 생성한 이메일 임베딩 벡터 (MariaDB VECTOR(384) 바이너리)
    @Column(name = "email_embedding")
    @Convert(converter = VectorConverter.class)
    private float[] emailEmbedding;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 분석 결과 업데이트 (AI 서버 응답 수신 후 호출)
    public void updateAnalysisResult(String domain, String intent,
                                     BigDecimal confidence, String summary,
                                     Map<String, Object> entities, boolean scheduleDetected) {
        this.domain = domain;
        this.intent = intent;
        this.confidenceScore = confidence;
        this.summaryText = summary;
        this.entitiesJson = entities;
        this.scheduleDetected = scheduleDetected;
    }

    /**
     * classify 큐 AI 결과 수신 후 업데이트
     * emailEmbedding: float[] (MariaDB VECTOR(384) 바이너리로 저장)
     */
    public void updateFromClassify(String domain, String intent, BigDecimal confidenceScore,
                                   String summaryText, boolean scheduleDetected, float[] emailEmbedding) {
        this.domain = domain;
        this.intent = intent;
        this.confidenceScore = confidenceScore;
        this.summaryText = summaryText;
        this.scheduleDetected = scheduleDetected;
        this.emailEmbedding = emailEmbedding;
    }
}
