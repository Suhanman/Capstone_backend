package com.emailagent.rabbitmq.dto;

import com.emailagent.domain.entity.Outbox;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Outbox 레코드를 RabbitMQ 메시지로 직렬화하는 DTO.
 * AI 서버가 처리에 필요한 이메일 메타데이터와 본문을 포함한다.
 * AI 서버는 응답 시 outbox_id / email_id를 그대로 반환하여 상태 추적에 사용한다.
 */
@Getter
@Builder
public class OutboxPayloadDTO {

    @JsonProperty("outbox_id")
    private Long outboxId;

    @JsonProperty("email_id")
    private Long emailId;

    @JsonProperty("sender_email")
    private String senderEmail;

    @JsonProperty("sender_name")
    private String senderName;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("body_clean")
    private String bodyClean;

    @JsonProperty("received_at")
    private LocalDateTime receivedAt;

    /**
     * Outbox.payload 맵만으로 DTO 생성 (Outbox 패턴 준수)
     * Email 엔티티에 접근하지 않으므로 LazyInitializationException 방지.
     * 스케줄러가 폴링한 Outbox 데이터만으로 RabbitMQ 발행이 완결됨.
     */
    public static OutboxPayloadDTO from(Outbox outbox) {
        Map<String, Object> payload = outbox.getPayload();

        // payload["date"]는 ISO-8601 String → LocalDateTime 파싱
        String dateStr = (String) payload.get("date");
        LocalDateTime receivedAt = null;
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                receivedAt = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e) {
                // 파싱 실패 시 null 유지
            }
        }

        return OutboxPayloadDTO.builder()
                .outboxId(outbox.getOutboxId())
                .emailId(((Number) payload.get("email_id")).longValue())
                .senderEmail((String) payload.get("from"))
                .senderName((String) payload.get("sender_name"))
                .subject((String) payload.get("subject"))
                .bodyClean((String) payload.get("body_clean"))
                .receivedAt(receivedAt)
                .build();
    }
}
