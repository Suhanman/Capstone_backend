package com.emailagent.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * AI 서버의 분류(classify) 결과를 역직렬화하는 DTO.
 * q.2app.classify 큐에서 수신하며, EmailAnalysisResult 엔티티 업데이트에 사용된다.
 *
 * [필드 기준]
 * EmailAnalysisResult.updateFromClassify() 시그니처 및 엔티티 필드 기준으로 정의한다.
 */
@Getter
@NoArgsConstructor
public class ClassifyResultDTO {

    /** Outbox 상태 추적용 식별자 */
    @JsonProperty("outbox_id")
    private Long outboxId;

    /** 이메일 식별자 (EmailAnalysisResult 저장 대상) */
    @JsonProperty("email_id")
    private Long emailId;

    /** 도메인 분류 (예: "마케팅", "기술지원") */
    @JsonProperty("domain")
    private String domain;

    /** 의도 분류 (예: "광고 문의", "불만 접수") */
    @JsonProperty("intent")
    private String intent;

    /** 분류 신뢰도 점수 (0.00 ~ 1.00) */
    @JsonProperty("confidence_score")
    private BigDecimal confidenceScore;

    /** AI 생성 요약 텍스트 */
    @JsonProperty("summary_text")
    private String summaryText;

    /** 일정 감지 여부 */
    @JsonProperty("schedule_detected")
    private boolean scheduleDetected;

    /** 엔티티 추출 결과 - AI가 JSON 문자열로 전송하므로 String으로 수신 후 서비스 레이어에서 파싱 */
    @JsonProperty("entities_json")
    private String entitiesJson;

    /** AI 모델 버전 */
    @JsonProperty("model_version")
    private String modelVersion;
}
