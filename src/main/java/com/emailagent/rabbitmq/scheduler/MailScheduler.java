package com.emailagent.rabbitmq.scheduler;

import com.emailagent.domain.entity.Outbox;
import com.emailagent.rabbitmq.publisher.MailPublisher;
import com.emailagent.rabbitmq.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outbox 폴링 스케줄러.
 *
 * [역할]
 * 1. 10초마다 READY 상태 Outbox를 SKIP LOCKED로 조회 → SENDING 전환 → 발행.
 * 2. 30분마다 SENDING 상태 타임아웃 항목을 READY로 롤백.
 *
 * [분산 환경]
 * SKIP LOCKED 쿼리로 여러 Pod가 동시에 폴링해도 동일 Outbox를 중복 처리하지 않음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailScheduler {

    /** 폴링 1회당 처리할 최대 건수 */
    private static final int POLL_BATCH_SIZE = 50;

    private final MailService mailService;
    private final MailPublisher mailPublisher;

    /**
     * 10초마다 READY Outbox 폴링 및 RabbitMQ 발행.
     */
    @Scheduled(fixedDelay = 10_000)
    public void pollAndPublish() {
        // SKIP LOCKED + SENDING 전환 (트랜잭션 내에서 원자적으로 수행)
        List<Outbox> sendingList = mailService.markAsSendingBatch(POLL_BATCH_SIZE);

        if (sendingList.isEmpty()) {
            log.debug("[MailScheduler] 처리할 데이터가 없습니다.");
            return;
        }

        log.debug("[MailScheduler] 발행 대상 {}건", sendingList.size());
        for (Outbox outbox : sendingList) {
            try {
                mailPublisher.publish(outbox);
            } catch (Exception e) {
                log.error("[MailScheduler] 발행 예외 — outboxId={}, error={}", outbox.getOutboxId(), e.getMessage(), e);
                mailService.markPublishedFailed(outbox.getOutboxId());
            }
        }
    }

    /**
     * 30분마다 SENDING 타임아웃 Outbox → READY 롤백.
     * 서비스 서버 → RabbitMQ 전송 실패로 SENDING이 30분 이상 지속된 경우 복구.
     */
    @Scheduled(fixedDelay = 1_800_000)
    public void resetTimedOut() {
        log.debug("[MailScheduler] SENDING 타임아웃 롤백 실행");
        mailService.resetTimedOut();
    }
}
