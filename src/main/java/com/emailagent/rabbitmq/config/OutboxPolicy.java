package com.emailagent.rabbitmq.config;

/**
 * Outbox 재시도 정책 상수.
 *
 * App → RabbitMQ 발행 실패 최대 재시도 횟수와
 * AI 서버 → App 컨슈머 재시도 횟수를 단일 위치에서 관리한다.
 */
public final class OutboxPolicy {

    /** App → RabbitMQ 발행 실패 / AI 컨슈머 처리 실패 공통 최대 재시도 횟수 */
    public static final int MAX_RETRY = 3;

    private OutboxPolicy() {}
}
