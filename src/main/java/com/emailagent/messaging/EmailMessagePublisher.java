package com.emailagent.messaging;

import com.emailagent.config.RabbitMQConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    /**
     * AI 서버로 이메일 분류 요청 발행 → email.classify 큐
     * AI 서버는 이 메시지를 소비하여 도메인/의도 분류, 요약, 임베딩 처리 후 결과를 email.classify 큐에 발행
     */
    public void publishClassifyRequest(Long emailId, String subject, String body,
                                       String mailTone, String ragContext) {
        // request_id: req_{emailId}_{timestamp} 형식으로 생성
        String requestId = "req_" + emailId + "_" + System.currentTimeMillis();

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("request_id", requestId);
        message.put("emailId", emailId);
        message.put("subject", subject);
        message.put("body", body);
        message.put("mail_tone", mailTone);
        message.put("ragContext", ragContext);

        try {
            rabbitTemplate.convertAndSend(exchange, RabbitMQConfig.CLASSIFY_ROUTING_KEY, message);
            log.info("분류 요청 발행 완료: emailId={}, requestId={}", emailId, requestId);
        } catch (Exception e) {
            log.error("분류 요청 발행 실패: emailId={}, error={}", emailId, e.getMessage());
        }
    }

    /**
     * AI 서버로 초안 생성 요청 발행 → email.draft 큐
     * mode: "generate"(최초 생성) | "regenerate"(재생성)
     * previousDraft: regenerate 모드일 때만 포함, null이면 메시지에서 제외
     */
    public void publishDraftRequest(Long emailId, String subject, String body,
                                    String domain, String intent, String summary,
                                    String mailTone, String ragContext,
                                    String previousDraft, String mode) {
        String requestId = "req_" + emailId + "_" + System.currentTimeMillis();

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("request_id", requestId);
        message.put("emailId", emailId);
        message.put("subject", subject);
        message.put("body", body);
        message.put("domain", domain);
        message.put("intent", intent);
        message.put("summary", summary);
        message.put("mail_tone", mailTone);
        message.put("ragContext", ragContext);
        message.put("mode", mode);

        // previousDraft는 regenerate 모드일 때만 포함
        if (previousDraft != null) {
            message.put("previous_draft", previousDraft);
        }

        try {
            rabbitTemplate.convertAndSend(exchange, RabbitMQConfig.DRAFT_ROUTING_KEY, message);
            log.info("초안 생성 요청 발행 완료: emailId={}, mode={}, requestId={}", emailId, mode, requestId);
        } catch (Exception e) {
            log.error("초안 생성 요청 발행 실패: emailId={}, error={}", emailId, e.getMessage());
        }
    }
}
