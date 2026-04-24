package com.emailagent.rabbitmq.event;

import com.emailagent.rabbitmq.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * SSE Hub 알림을 RabbitMQ x.sse.fanout exchange로 publish하는 리스너.
 *
 * [설계 원칙]
 * - AFTER_COMMIT 보장: DB 트랜잭션 커밋 성공 후에만 MQ publish.
 *   이메일 저장 실패 시 SSE 알림이 나가지 않음.
 * - Fire-and-forget: SSE 알림 실패는 서비스 로직에 영향 없음.
 *   publish 실패 시 에러 로그만 기록.
 * - sseFanoutRabbitTemplate (mandatory=false): fanout exchange에 SSE Hub 큐가
 *   바인딩되지 않은 환경에서 ReturnsCallback 에러 로그가 발생하지 않도록 별도 Bean 사용.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseFanoutPublisher {

    // RabbitMQConfig에서 등록한 mandatory=false 전용 Bean (이름 기반 매칭)
    private final RabbitTemplate sseFanoutRabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSsePubSubEvent(SseEvent event) {
        // SSE Hub가 브라우저 연결을 식별·라우팅하기 위한 최소 payload
        Map<String, Object> payload = Map.of(
                "user_id",  event.getUserId(),
                "sse_type", event.getSseType(),
                "data", event.getData() != null ? event.getData() : Map.of()
        );

        try {
            // fanout exchange: routing key 불필요 → 빈 문자열
            sseFanoutRabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_SSE_FANOUT,
                    "",
                    payload
            );
            log.debug("[SseFanoutPublisher] publish 완료 — userId={}, sseType={}",
                    event.getUserId(), event.getSseType());
        } catch (Exception e) {
            // SSE 알림 실패는 핵심 서비스 흐름에 영향 없음 → 로그만 기록
            log.error("[SseFanoutPublisher] publish 실패 — userId={}, sseType={}, error={}",
                    event.getUserId(), event.getSseType(), e.getMessage(), e);
        }
    }
}
