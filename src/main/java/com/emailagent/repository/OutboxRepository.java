package com.emailagent.repository;

import com.emailagent.domain.entity.Outbox;
import com.emailagent.domain.enums.OutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    List<Outbox> findByStatusAndRetryCountLessThan(OutboxStatus status, int maxRetry);

    /**
     * 분산 Pod 환경에서 중복 폴링 방지: SKIP LOCKED
     * 다른 Pod가 잠근 행은 건너뛰고, 이 Pod가 처리할 수 있는 READY 항목만 가져옴
     */
    @Query(value = "SELECT * FROM outbox WHERE status = 'READY' ORDER BY created_at LIMIT :limit FOR UPDATE SKIP LOCKED",
           nativeQuery = true)
    List<Outbox> findReadyWithSkipLocked(@Param("limit") int limit);

    /**
     * 원자적 UPDATE: SELECT FOR UPDATE SKIP LOCKED + UPDATE를 한 트랜잭션 내에서 수행
     * Pod 환경에서 다중 Pod의 중복 폴링과 동시성 충돌을 완벽히 방지
     * (SELECT lock이 유지된 상태에서 UPDATE가 즉시 실행되므로 중간 간섭 불가)
     */
    @Modifying
    @Query(value = """
            UPDATE outbox
            SET status = 'SENDING', sent_at = NOW()
            WHERE outbox_id IN (
                SELECT outbox_id FROM (
                    SELECT outbox_id FROM outbox
                    WHERE status = 'READY'
                    ORDER BY created_at ASC
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                ) AS temp
            )
            """, nativeQuery = true)
    int updateReadyToSending(@Param("limit") int limit);

    /**
     * 방금 UPDATE된 항목들을 조회 (sent_at 기준으로 최근 1초 이내)
     * markAsSendingBatch()에서 updateReadyToSending() 후 반환할 항목 조회용
     */
    @Query(value = """
            SELECT * FROM outbox
            WHERE status = 'SENDING'
            AND sent_at > DATE_SUB(NOW(), INTERVAL 1 SECOND)
            ORDER BY sent_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Outbox> findRecentlySending(@Param("limit") int limit);

    // 타임아웃 감지: SENDING 상태인데 일정 시간 이상 경과
    @Query("SELECT o FROM Outbox o WHERE o.status = 'SENDING' AND o.sentAt < :timeout")
    List<Outbox> findTimedOutMessages(LocalDateTime timeout);

    // 관리자 - 전체 작업 목록 페이징 (email FETCH JOIN으로 N+1 방지)
    @Query(value = "SELECT o FROM Outbox o JOIN FETCH o.email ORDER BY o.createdAt DESC",
           countQuery = "SELECT COUNT(o) FROM Outbox o")
    Page<Outbox> findAllWithEmailOrderByCreatedAtDesc(Pageable pageable);

    // 관리자 - 상태 필터 페이징 (email FETCH JOIN)
    @Query(value = "SELECT o FROM Outbox o JOIN FETCH o.email WHERE o.status = :status ORDER BY o.createdAt DESC",
           countQuery = "SELECT COUNT(o) FROM Outbox o WHERE o.status = :status")
    Page<Outbox> findByStatusWithEmailOrderByCreatedAtDesc(@Param("status") OutboxStatus status, Pageable pageable);

    // 관리자 - 상태별 건수
    long countByStatus(OutboxStatus status);

    // 이메일별 활성 Outbox 조회 — 소프트 삭제 시 READY/SENDING 항목을 CANCELLED 처리하기 위해 사용
    List<Outbox> findByEmail_EmailIdAndStatusIn(Long emailId, List<OutboxStatus> statuses);
}
