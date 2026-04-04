package com.emailagent.rabbitmq.service;

import com.emailagent.domain.entity.Outbox;
import com.emailagent.dto.response.admin.operation.AdminJobDetailResponse;
import com.emailagent.dto.response.admin.operation.AdminJobListResponse;
import com.emailagent.dto.response.admin.operation.AdminJobSummaryResponse;
import com.emailagent.rabbitmq.dto.ClassifyResultDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * RabbitMQ 파이프라인의 트랜잭션 및 상태 관리 인터페이스.
 * 모든 Outbox 상태 전이는 이 인터페이스를 통해서만 수행한다.
 */
public interface MailService {

    /**
     * READY → SENDING 상태 전이 (스케줄러 폴링 후 호출)
     * 분산 Pod 중복 폴링 방지: SKIP LOCKED 쿼리로 가져온 항목만 전달받음.
     *
     * @return SENDING으로 전환된 Outbox 목록 (Publisher에게 전달)
     */
    List<Outbox> markAsSendingBatch(int limit);

    /**
     * Publisher Confirm ack=false 시 SENDING → READY 롤백.
     * RabbitMQ 브로커로 메시지 자체가 전달되지 않은 경우에만 호출.
     */
    void markPublishedFailed(Long outboxId);

    /**
     * Consumer ack 성공 후 SENDING → FINISH 전이 및 AI 결과 저장.
     * 트랜잭션 커밋 후 @TransactionalEventListener로 SSE 브로드캐스트 발화.
     */
    void markFinished(ClassifyResultDTO result);

    /**
     * Consumer x-death count >= 3 이후 SENDING → FAILED 확정.
     * q.dlx.failed로 메시지를 이동시킨 뒤 호출.
     */
    void markFailed(Long outboxId, String reason);

    /**
     * SENDING 상태가 30분 이상 지속된 Outbox → READY 롤백 (타임아웃 복구).
     * 스케줄러에서 주기적으로 호출.
     */
    void resetTimedOut();

    // ===================================================
    // 관리자 Job 조회 API 용
    // ===================================================

    AdminJobListResponse getJobList(String status, Pageable pageable);

    AdminJobSummaryResponse getJobSummary();

    AdminJobDetailResponse getJobDetail(Long outboxId);

    void forceDelete(Long outboxId);
}
