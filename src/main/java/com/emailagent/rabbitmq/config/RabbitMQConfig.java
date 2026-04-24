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
 * - Exchange / Queue / Binding 등 RabbitMQ 리소스는 인프라에서 관리.
 * - 로컬 smoke test에서도 운영과 같은 원칙으로 별도 setup script / RabbitMQ definitions에서 생성한다.
 * - 운영형 DLX 설정, TTL 등 브로커 리소스 속성은 앱 코드에서 선언하지 않음.
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    // ===================================================
    // 교환기 이름 상수 (Terraform 관리 값과 일치해야 함)
    // ===================================================
    public static final String EXCHANGE_APP2AI = "x.app2ai.direct";
    public static final String EXCHANGE_AI2APP = "x.ai2app.direct";
    public static final String EXCHANGE_APP2RAG = "x.app2rag.direct";
    public static final String EXCHANGE_RAG2APP = "x.rag2app.direct";
    public static final String EXCHANGE_SSE_FANOUT = "x.sse.fanout";

    // ===================================================
    // 큐 이름 상수
    // ===================================================
    public static final String QUEUE_CLASSIFY_INBOUND  = "q.2ai.classify";
    public static final String QUEUE_CLASSIFY_RESULT   = "q.2app.classify";
    public static final String QUEUE_KNOWLEDGE_INGEST_INBOUND = "q.2rag.knowledge.ingest";
    public static final String QUEUE_KNOWLEDGE_INGEST_RESULT  = "q.2app.knowledge.ingest";
    public static final String QUEUE_RAG_DRAFT_INBOUND = "q.2rag.draft";
    public static final String QUEUE_RAG_DRAFT_RESULT  = "q.2app.rag.draft";
    public static final String QUEUE_TEMPLATE_INDEX_INBOUND = "q.2rag.templates.index";
    public static final String QUEUE_TEMPLATE_INDEX_RESULT  = "q.2app.templates.index";
    public static final String QUEUE_TEMPLATE_MATCH_INBOUND = "q.2rag.templates.match";
    public static final String QUEUE_TEMPLATE_MATCH_RESULT  = "q.2app.templates.match";
    public static final String QUEUE_RAG_PROGRESS      = "q.2app.rag.progress";
    public static final String QUEUE_DLX_FAILED        = "q.dlx.failed";
    public static final String QUEUE_TRAINING_RESULT   = "q.2app.training";

    // ===================================================
    // 라우팅 키 상수
    // ===================================================
    public static final String RK_CLASSIFY_INBOUND = "2ai.classify";
    public static final String RK_CLASSIFY_RESULT  = "2app.classify";
    public static final String RK_KNOWLEDGE_INGEST_INBOUND = "2rag.knowledge.ingest";
    public static final String RK_KNOWLEDGE_INGEST_RESULT  = "2app.knowledge.ingest";
    public static final String RK_RAG_DRAFT_INBOUND = "2rag.draft";
    public static final String RK_RAG_DRAFT_RESULT  = "2app.rag.draft";
    public static final String RK_TEMPLATE_INDEX_INBOUND = "2rag.templates.index";
    public static final String RK_TEMPLATE_INDEX_RESULT  = "2app.templates.index";
    public static final String RK_TEMPLATE_MATCH_INBOUND = "2rag.templates.match";
    public static final String RK_TEMPLATE_MATCH_RESULT  = "2app.templates.match";
    public static final String RK_RAG_PROGRESS      = "2app.rag.progress";
    public static final String RK_DRAFT_INBOUND    = "2ai.draft";
    public static final String RK_DRAFT_RESULT     = "2app.draft";

    // ===================================================
    // 메시지 JSON 컨버터
    // ===================================================
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        // 파이썬 AI 서버는 Spring의 __TypeId__ 헤더를 포함하지 않으므로,
        // 헤더 대신 @RabbitListener 메서드 파라미터 타입으로 역직렬화 대상을 추론하도록 설정.
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }

    // ===================================================
    // 메시지 발행 템플릿 - 발행 확인/반환 콜백 설정
    // ===================================================
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);

        // 발행 확인: 브로커가 메시지를 수신했는지 확인
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("[RabbitMQ] Publisher Confirm NACK — correlationId={}, cause={}",
                        correlationData != null ? correlationData.getId() : "unknown", cause);
            }
        });

        // 발행 반환: 라우팅 실패 시 메시지 반환 콜백
        template.setReturnsCallback(returned -> log.error(
                "[RabbitMQ] Message returned — exchange={}, routingKey={}, replyCode={}, replyText={}",
                returned.getExchange(), returned.getRoutingKey(),
                returned.getReplyCode(), returned.getReplyText()));

        // 필수 라우팅 설정(mandatory=true): 라우팅 실패 시 반환 콜백 발동
        template.setMandatory(true);

        return template;
    }

    // ===================================================
    // 관리용 RabbitAdmin - 초기화 시점에 교환기/큐 passive 선언으로 존재 여부 확인
    // ===================================================
    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        // 수동 확인 선언(passive) → Terraform이 미리 생성한 리소스가 없으면 기동 실패로 조기 감지
        admin.setAutoStartup(true);
        return admin;
    }

    // ===================================================
    // 실시간 알림 팬아웃 전용 메시지 발행 템플릿 (mandatory=false)
    // ===================================================
    /**
     * 실시간 알림 허브 전용 RabbitTemplate.
     *
     * 필수 라우팅 해제(mandatory=false) 설정으로 팬아웃 교환기에 큐가 바인딩되지 않은 경우(SSE Hub 미기동 등)
     * 반환 콜백이 발동되어 에러 로그가 찍히는 운영 노이즈를 방지한다.
     * 실시간 알림은 영속성 보장이 불필요하므로 발행 확인 콜백도 등록하지 않는다.
     */
    @Bean
    public RabbitTemplate sseFanoutRabbitTemplate(ConnectionFactory connectionFactory,
                                                   Jackson2JsonMessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(false);
        return template;
    }

    // ===================================================
    // 컨슈머 컨테이너 팩토리 (수동 ack + prefetch=1)
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
        // 수동 ack: 컨슈머가 직접 basicAck/basicNack 호출
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        return factory;
    }
}
