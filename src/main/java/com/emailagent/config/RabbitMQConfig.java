package com.emailagent.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 큐/라우팅 키 상수 (Consumer 어노테이션에서 참조)
    public static final String CLASSIFY_QUEUE = "email.classify";
    public static final String DRAFT_QUEUE = "email.draft";
    public static final String CLASSIFY_ROUTING_KEY = "email.classify";
    public static final String DRAFT_ROUTING_KEY = "email.draft";

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue classifyQueue() {
        return QueueBuilder.durable(CLASSIFY_QUEUE).build();
    }

    @Bean
    public Queue draftQueue() {
        return QueueBuilder.durable(DRAFT_QUEUE).build();
    }

    @Bean
    public Binding classifyBinding() {
        return BindingBuilder.bind(classifyQueue())
                .to(emailExchange())
                .with(CLASSIFY_ROUTING_KEY);
    }

    @Bean
    public Binding draftBinding() {
        return BindingBuilder.bind(draftQueue())
                .to(emailExchange())
                .with(DRAFT_ROUTING_KEY);
    }

    // JSON 직렬화 컨버터
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
