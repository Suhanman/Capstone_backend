package com.emailagent.repository;

import com.emailagent.domain.entity.RagJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RagJobRepository extends JpaRepository<RagJob, String> {
    List<RagJob> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
}
