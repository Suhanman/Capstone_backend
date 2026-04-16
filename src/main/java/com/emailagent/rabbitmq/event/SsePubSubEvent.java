package com.emailagent.rabbitmq.event;

import org.springframework.context.ApplicationEvent;

/**
 * Gmail Pub/Sub 처리 완료 후 SSE Hub에 알릴 이벤트.
 *
 * PubSubHandlerService.handleAsync() 트랜잭션 커밋 직전에 발행되며,
 * SseFanoutPublisher가 @TransactionalEventListener(AFTER_COMMIT)으로 수신하여
 * x.sse.fanout exchange에 publish한다.
 *
 * [필드 설계]
 * - userId: SSE Hub가 어느 브라우저 연결에 push할지 결정하는 식별자
 * - sseType: 이벤트 종류 구분 ("pub/sub", 향후 "classify" / "draft" 추가 가능)
 */
public class SsePubSubEvent extends ApplicationEvent {

    private final Long userId;
    private final String sseType;

    public SsePubSubEvent(Object source, Long userId, String sseType) {
        super(source);
        this.userId = userId;
        this.sseType = sseType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSseType() {
        return sseType;
    }
}
