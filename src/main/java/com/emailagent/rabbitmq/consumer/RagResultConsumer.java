package com.emailagent.rabbitmq.consumer;

import com.emailagent.rabbitmq.config.OutboxPolicy;
import com.emailagent.rabbitmq.config.RabbitMQConfig;
import com.emailagent.rabbitmq.dto.RagDraftGenerateResultDTO;
import com.emailagent.service.RagJobService;
import com.emailagent.service.RagResultService;
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
 * RAG -> App 결과 메시지 수신 컨슈머.
 *
 * 현재는 온보딩 템플릿 생성 결과를 받아 Template 저장 및 templates.index 발행까지 담당한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagResultConsumer {

    private final RagResultService ragResultService;
    private final RagJobService ragJobService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(
            queues = RabbitMQConfig.QUEUE_RAG_DRAFT_RESULT,
            ackMode = "MANUAL",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consumeDraftResult(RagDraftGenerateResultDTO result, Message message, Channel channel)
            throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            int retryCount = extractRetryCount(message);

            if (retryCount >= OutboxPolicy.MAX_RETRY) {
                log.error(
                        "[RagResultConsumer] 최대 재시도 초과 → FAILED — requestId={}, jobId={}, retryCount={}",
                        result.getRequestId(),
                        result.getJobId(),
                        retryCount
                );
                rabbitTemplate.send(RabbitMQConfig.QUEUE_DLX_FAILED, message);
                channel.basicAck(deliveryTag, false);
                return;
            }

            ragResultService.handleDraftGenerated(result);
            ragJobService.completeDraftGeneration(result);
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error(
                    "[RagResultConsumer] draft result 처리 실패 → nack — requestId={}, jobId={}, error={}",
                    result.getRequestId(),
                    result.getJobId(),
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
