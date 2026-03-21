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

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.queue.request}")
    private String requestQueue;

    @Value("${app.rabbitmq.queue.response}")
    private String responseQueue;

    @Value("${app.rabbitmq.routing-key.request}")
    private String requestRoutingKey;

    @Value("${app.rabbitmq.routing-key.response}")
    private String responseRoutingKey;

    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue emailRequestQueue() {
        return QueueBuilder.durable(requestQueue).build();
    }

    @Bean
    public Queue emailResponseQueue() {
        return QueueBuilder.durable(responseQueue).build();
    }

    @Bean
    public Binding requestBinding() {
        return BindingBuilder.bind(emailRequestQueue())
                .to(emailExchange())
                .with(requestRoutingKey);
    }

    @Bean
    public Binding responseBinding() {
        return BindingBuilder.bind(emailResponseQueue())
                .to(emailExchange())
                .with(responseRoutingKey);
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
