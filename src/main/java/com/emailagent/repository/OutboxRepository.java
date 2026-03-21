package com.emailagent.repository;

import com.emailagent.domain.entity.Outbox;
import com.emailagent.domain.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    List<Outbox> findByStatusAndRetryCountLessThan(OutboxStatus status, int maxRetry);

    // 타임아웃 감지: SENDING 상태인데 일정 시간 이상 경과
    @Query("SELECT o FROM Outbox o WHERE o.status = 'SENDING' AND o.sentAt < :timeout")
    List<Outbox> findTimedOutMessages(LocalDateTime timeout);
}
