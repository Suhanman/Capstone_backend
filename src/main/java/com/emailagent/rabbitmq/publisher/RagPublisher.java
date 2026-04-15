package com.emailagent.rabbitmq.publisher;

import com.emailagent.rabbitmq.config.RabbitMQConfig;
import com.emailagent.rabbitmq.dto.RagDraftGenerateRequestDTO;
import com.emailagent.rabbitmq.dto.RagKnowledgeIngestRequestDTO;
import com.emailagent.rabbitmq.dto.RagTemplateMatchRequestDTO;
import com.emailagent.rabbitmq.dto.RagTemplateIndexRequestDTO;
import com.emailagent.service.RagJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * App Server -> RAG RabbitMQ 발행 컴포넌트.
 *
 * 현재는 온보딩 기준으로 knowledge ingest 와 draft generation 발행을 담당한다.
 * 이후 templates.index / templates.match 도 같은 패턴으로 확장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RagJobService ragJobService;

    public void publishKnowledgeIngest(RagKnowledgeIngestRequestDTO payload) {
        ragJobService.createKnowledgeIngestJob(payload);
        CorrelationData correlationData = new CorrelationData(payload.getRequestId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_APP2RAG,
                RabbitMQConfig.RK_KNOWLEDGE_INGEST_INBOUND,
                payload,
                correlationData
        );

        log.info(
                "[RagPublisher] knowledge ingest 발행 완료 — userId={}, jobId={}, requestId={}",
                payload.getUserId(),
                payload.getJobId(),
                payload.getRequestId()
        );
    }

    public void publishDraftGeneration(RagDraftGenerateRequestDTO payload) {
        ragJobService.createDraftGenerationJob(payload);
        CorrelationData correlationData = new CorrelationData(payload.getRequestId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_APP2RAG,
                RabbitMQConfig.RK_RAG_DRAFT_INBOUND,
                payload,
                correlationData
        );

        log.info(
                "[RagPublisher] draft generation 발행 완료 — userId={}, categoryId={}, jobId={}, requestId={}",
                payload.getUserId(),
                payload.getCategoryId(),
                payload.getJobId(),
                payload.getRequestId()
        );
    }

    public void publishTemplateIndex(RagTemplateIndexRequestDTO payload) {
        CorrelationData correlationData = new CorrelationData(payload.getRequestId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_APP2RAG,
                RabbitMQConfig.RK_TEMPLATE_INDEX_INBOUND,
                payload,
                correlationData
        );

        log.info(
                "[RagPublisher] template index 발행 완료 — userId={}, requestId={}",
                payload.getUserId(),
                payload.getRequestId()
        );
    }

    public void publishTemplateMatch(RagTemplateMatchRequestDTO payload) {
        ragJobService.createTemplateMatchJob(payload);
        CorrelationData correlationData = new CorrelationData(payload.getRequestId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_APP2RAG,
                RabbitMQConfig.RK_TEMPLATE_MATCH_INBOUND,
                payload,
                correlationData
        );

        log.info(
                "[RagPublisher] template match 발행 완료 — userId={}, emailId={}, jobId={}, requestId={}",
                payload.getUserId(),
                payload.getPayload() != null ? payload.getPayload().getEmailId() : null,
                payload.getJobId(),
                payload.getRequestId()
        );
    }
}
