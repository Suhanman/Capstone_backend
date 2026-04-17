package com.emailagent.repository;

import com.emailagent.domain.entity.TrainingJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingJobRepository extends JpaRepository<TrainingJob, String> {
}
