package com.emailagent.rabbitmq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 설정
 *
 * [설계 원칙]
 * - Exchange / Queue / Binding 등 RabbitMQ 리소스는 Terraform에서 관리.
 * - Spring Boot는 리소스 생성 권한이 없으며, passive 선언(존재 여부 확인)만 수행.
 * - DLX 설정, TTL 등 브로커 리소스 속성은 일체 선언하지 않음.
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    // ===================================================
    // Exchange 이름 상수 (Terraform 관리 값과 일치해야 함)
    // ===================================================
    public static final String EXCHANGE_APP2AI = "x.app2ai.direct";
    public static final String EXCHANGE_AI2APP = "x.ai2app.direct";

    // ===================================================
    // Queue 이름 상수
    // ===================================================
    public static final String QUEUE_CLASSIFY_INBOUND  = "q.2ai.classify";
    public static final String QUEUE_CLASSIFY_RESULT   = "q.2app.classify";
    public static final String QUEUE_DRAFT_INBOUND     = "q.2ai.draft";
    public static final String QUEUE_DRAFT_RESULT      = "q.2app.draft";
    public static final String QUEUE_DLX_FAILED        = "q.dlx.failed";
    public static final String QUEUE_TRAINING_RESULT   = "q.2app.training";

    // ===================================================
    // Routing Key 상수
    // ===================================================
    public static final String RK_CLASSIFY_INBOUND = "2ai.classify";
    public static final String RK_CLASSIFY_RESULT  = "2app.classify";
    public static final String RK_DRAFT_INBOUND    = "2ai.draft";
    public static final String RK_DRAFT_RESULT     = "2app.draft";
    // ===================================================
    // JSON 메시지 컨버터
    // ===================================================
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ===================================================
    // RabbitTemplate - Publisher Confirms/Returns 콜백 설정
    // ===================================================
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);

        // Publisher Confirms: 브로커가 메시지를 수신했는지 확인
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("[RabbitMQ] Publisher Confirm NACK — correlationId={}, cause={}",
                        correlationData != null ? correlationData.getId() : "unknown", cause);
            }
        });

        // Publisher Returns: 라우팅 실패 시 메시지 반환 콜백
        template.setReturnsCallback(returned -> log.error(
                "[RabbitMQ] Message returned — exchange={}, routingKey={}, replyCode={}, replyText={}",
                returned.getExchange(), returned.getRoutingKey(),
                returned.getReplyCode(), returned.getReplyText()));

        // mandatory=true: 라우팅 실패 시 ReturnsCallback 발동
        template.setMandatory(true);

        return template;
    }

    // ===================================================
    // RabbitAdmin - 초기화 시점에 Exchange/Queue passive 선언으로 존재 여부 확인
    // ===================================================
    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        // passive 선언 → Terraform이 미리 생성한 리소스가 없으면 기동 실패로 조기 감지
        admin.setAutoStartup(true);
        return admin;
    }

    // ===================================================
    // Consumer 컨테이너 팩토리 (MANUAL ack + prefetch=1)
    // ===================================================
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        // 한 번에 1개 메시지만 처리: 분산 환경에서 순서 및 중복 방지
        factory.setPrefetchCount(1);
        // MANUAL ack: Consumer가 직접 basicAck/basicNack 호출
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        return factory;
    }
}
