package com.emailagent.rabbitmq.consumer;

import com.emailagent.rabbitmq.config.OutboxPolicy;
import com.emailagent.rabbitmq.config.RabbitMQConfig;
import com.emailagent.rabbitmq.dto.ClassifyResultDTO;
import com.emailagent.rabbitmq.service.MailService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * AI 서버 → App 서버 분류 결과 수신 컨슈머.
 *
 * [처리 흐름]
 * 성공: 수신 → markFinished() [트랜잭션 커밋] → AFTER_COMMIT SSE 브로드캐스트 → basicAck
 * 실패(retry < 3): nack(requeue=false) → DLX → retry 큐 → 30s 후 재투입
 * 실패(retry >= 3): markFailed() → q.dlx.failed 이동 → basicAck
 *
 * [Prefetch]
 * prefetch=1: 한 번에 1개 메시지만 처리하여 중복/순서 문제 방지.
 *
 * [멱등성]
 * markFinished()는 이미 FINISH 상태인 경우 조용히 무시하여 AI 서버 중복 발행을 방어.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailConsumer {


    private final MailService mailService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(
            queues = RabbitMQConfig.QUEUE_CLASSIFY_RESULT,
            ackMode = "MANUAL",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consumeClassifyResult(ClassifyResultDTO result, Message message, Channel channel)
            throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        Long outboxId = result.getOutboxId();

        try {
            // x-death 헤더에서 재시도 횟수 읽기
            int retryCount = extractRetryCount(message);

            if (retryCount >= OutboxPolicy.MAX_RETRY) {
                // 최대 재시도 초과 → FAILED 확정 후 q.dlx.failed로 이동
                // FAILED 블록 내부 예외가 외부 catch로 탈출하면 basicNack → 재시도 무한반복이 되므로
                // 독립 try-catch로 감싸서 예외 여부와 무관하게 반드시 basicAck 처리
                log.error("[MailConsumer] 최대 재시도 초과 → FAILED — outboxId={}, retryCount={}", outboxId, retryCount);
                try {
                    if (outboxId != null) {
                        mailService.markFailed(outboxId, "Max retry exceeded: " + retryCount);
                    } else {
                        log.error("[MailConsumer] outboxId 없음(AI 서버 페이로드 미포함) — 메시지 폐기만 수행, retryCount={}", retryCount);
                    }
                    rabbitTemplate.send(RabbitMQConfig.QUEUE_DLX_FAILED, message);
                } catch (Exception ex) {
                    log.error("[MailConsumer] FAILED 처리 중 예외 — q.dlx.failed 전송 실패 가능성, outboxId={}, error={}", outboxId, ex.getMessage(), ex);
                } finally {
                    // 예외와 무관하게 반드시 ack → 재시도 루프 차단
                    channel.basicAck(deliveryTag, false);
                }
                return;
            }

            // 정상 처리
            mailService.markFinished(result);
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[MailConsumer] 처리 실패 → nack — outboxId={}, error={}", outboxId, e.getMessage(), e);
            // requeue=false: DLX로 이동 → retry 큐 → 30s 후 재투입
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * x-death 헤더에서 재시도 횟수 추출.
     * x-death 배열의 첫 번째 항목의 count 값을 읽는다.
     */
    @SuppressWarnings("unchecked")
    private int extractRetryCount(Message message) {
        Object xDeath = message.getMessageProperties().getHeaders().get("x-death");
        if (xDeath instanceof List<?> xDeathList && !xDeathList.isEmpty()) {
            Object first = xDeathList.get(0);
            if (first instanceof Map<?, ?> deathMap) {
                Object count = deathMap.get("count");
                if (count instanceof Number num) {
                    return num.intValue();
                }
            }
        }
        return 0;
    }
}
