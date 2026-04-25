package com.emailagent.rabbitmq.consumer;

import com.emailagent.rabbitmq.config.OutboxPolicy;
import com.emailagent.rabbitmq.config.RabbitMQConfig;
import com.emailagent.rabbitmq.dto.RagProgressEventDTO;
import com.emailagent.service.RagJobService;
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
 * RAG progress 이벤트 수신 컨슈머.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagProgressConsumer {

    private final RagJobService ragJobService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(
            queues = RabbitMQConfig.QUEUE_RAG_PROGRESS,
            ackMode = "MANUAL",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consumeProgress(RagProgressEventDTO progress, Message message, Channel channel)
            throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            int retryCount = extractRetryCount(message);
            if (retryCount >= OutboxPolicy.MAX_RETRY) {
                log.error(
                        "[RagProgressConsumer] 최대 재시도 초과 → DLQ 이동 — requestId={}, jobId={}, retryCount={}",
                        progress.getRequestId(),
                        progress.getJobId(),
                        retryCount
                );
                rabbitTemplate.send(RabbitMQConfig.QUEUE_DLX_FAILED, message);
                channel.basicAck(deliveryTag, false);
                return;
            }

            ragJobService.applyProgress(progress);
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error(
                    "[RagProgressConsumer] progress 처리 실패 → nack — requestId={}, jobId={}, error={}",
                    progress.getRequestId(),
                    progress.getJobId(),
                    e.getMessage(),
                    e
            );
            channel.basicNack(deliveryTag, false, false);
        }
    }

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
