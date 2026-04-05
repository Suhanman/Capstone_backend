package com.emailagent.rabbitmq.publisher;

import com.emailagent.domain.entity.Outbox;
import com.emailagent.rabbitmq.config.RabbitMQConfig;
import com.emailagent.rabbitmq.dto.OutboxPayloadDTO;
import com.emailagent.rabbitmq.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Outbox → RabbitMQ 발행 컴포넌트.
 *
 * [역할]
 * - 스케줄러에서 SENDING 상태로 전환된 Outbox를 받아 x.app2ai.direct에 발행.
 * - CorrelationData에 outboxId 적재: Publisher Confirm 콜백에서 실패 시 추적에 사용.
 * - ack=false(브로커 미전달) 시 MailService.markPublishedFailed() 호출.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MailService mailService;

    /**
     * 단건 Outbox를 classify 큐에 발행.
     *
     * @param outbox SENDING 상태의 Outbox 엔티티
     */
    public void publish(Outbox outbox) {
        Long outboxId = outbox.getOutboxId();
        OutboxPayloadDTO payload = OutboxPayloadDTO.from(outbox);

        // CorrelationData에 outboxId 적재: ConfirmCallback에서 ack/nack 구분 시 사용
        CorrelationData correlationData = new CorrelationData(String.valueOf(outboxId));
        correlationData.getFuture().whenComplete((confirm, ex) -> {
            if (ex != null) {
                log.error("[MailPublisher] ConfirmCallback 예외 — outboxId={}, error={}", outboxId, ex.getMessage());
                mailService.markPublishedFailed(outboxId);
                return;
            }
            if (confirm != null && !confirm.isAck()) {
                // ack=false: 브로커가 메시지를 받지 못한 경우 → READY로 롤백
                log.warn("[MailPublisher] Publisher Confirm ack=false — outboxId={}, reason={}", outboxId, confirm.getReason());
                mailService.markPublishedFailed(outboxId);
            }
        });

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_APP2AI,
                RabbitMQConfig.RK_CLASSIFY_INBOUND,
                payload,
                correlationData
        );

        log.debug("[MailPublisher] 발행 완료 — outboxId={}, emailId={}", outboxId, outbox.getEmail().getEmailId());
    }
}
