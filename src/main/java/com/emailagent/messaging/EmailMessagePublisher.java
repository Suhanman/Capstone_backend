package com.emailagent.messaging;

import com.emailagent.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final OutboxRepository outboxRepository;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.request}")
    private String requestRoutingKey;

    /**
     * AI 서버로 이메일 분석 요청 발행 (STEP 0 → AI 처리 시작)
     */
    public void publishEmailAnalysisRequest(Long outboxId, Map<String, Object> payload) {
        try {
            // Outbox 상태를 SENDING으로 변경
            outboxRepository.findById(outboxId).ifPresent(outbox -> {
                outbox.markAsSending();
                outboxRepository.save(outbox);
            });

            rabbitTemplate.convertAndSend(exchange, requestRoutingKey, payload);
            log.info("AI 분석 요청 발행 완료: outboxId={}", outboxId);

        } catch (Exception e) {
            log.error("메시지 발행 실패: outboxId={}, error={}", outboxId, e.getMessage());
            outboxRepository.findById(outboxId).ifPresent(outbox -> {
                outbox.markAsFailed(e.getMessage());
                outboxRepository.save(outbox);
            });
        }
    }
}
