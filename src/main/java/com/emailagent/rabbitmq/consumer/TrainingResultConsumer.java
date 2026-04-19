package com.emailagent.rabbitmq.consumer;

import com.emailagent.rabbitmq.config.RabbitMQConfig;
import com.emailagent.rabbitmq.dto.TrainingJobResultMessage;
import com.emailagent.service.admin.AiTrainingService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * AI Training Worker → Backend 완료 이벤트 수신 컨슈머.
 * q.2app.training 큐에서 consume하여 training_jobs 상태 업데이트.
 *
 * [처리 흐름]
 * 성공: consume → handleTrainingResult() → COMPLETED/FAILED 업데이트 → basicAck
 * 실패: 예외 발생 → 로그 → basicNack(requeue=false)
 *
 * 초기 단계: retry/DLQ 없이 단순 처리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrainingResultConsumer {

    private final AiTrainingService aiTrainingService;

    @RabbitListener(
            queues = RabbitMQConfig.QUEUE_TRAINING_RESULT,
            ackMode = "MANUAL",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consumeTrainingResult(TrainingJobResultMessage result, Message message, Channel channel)
            throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String jobId = result.getJobId();

        try {
            aiTrainingService.handleTrainingResult(result);
            channel.basicAck(deliveryTag, false);
            log.info("[TrainingResultConsumer] 처리 완료 — jobId={}, status={}", jobId, result.getStatus());
        } catch (Exception e) {
            log.error("[TrainingResultConsumer] 처리 실패 — jobId={}, error={}", jobId, e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
