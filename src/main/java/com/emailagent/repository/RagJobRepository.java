package com.emailagent.repository;

import com.emailagent.domain.entity.RagJob;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RagJobRepository extends JpaRepository<RagJob, String> {
    List<RagJob> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
    List<RagJob> findByUser_UserIdAndJobIdInOrderByCreatedAtAsc(Long userId, List<String> jobIds);

    // 분산 환경 동시 업데이트 경쟁 방지용 SELECT FOR UPDATE
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM RagJob j WHERE j.jobId = :jobId")
    Optional<RagJob> findByIdForUpdate(@Param("jobId") String jobId);
}
