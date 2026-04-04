package com.emailagent.rabbitmq.event;

import org.springframework.context.ApplicationEvent;

/**
 * AI 분류 결과 저장 완료 후 SSE Pod 브로드캐스트를 위한 이벤트.
 * MailServiceImpl.markFinished() 트랜잭션 커밋 직후 발행.
 * SseBroadcastListener에서 @TransactionalEventListener(AFTER_COMMIT)으로 수신.
 */
public class SseNotifyEvent extends ApplicationEvent {

    private final Long emailId;

    public SseNotifyEvent(Object source, Long emailId) {
        super(source);
        this.emailId = emailId;
    }

    public Long getEmailId() {
        return emailId;
    }
}
