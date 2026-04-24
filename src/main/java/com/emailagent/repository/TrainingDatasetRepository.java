package com.emailagent.repository;

import com.emailagent.domain.entity.TrainingDataset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingDatasetRepository extends JpaRepository<TrainingDataset, Long> {

    /** 생성 시각 최신순 전체 목록 */
    List<TrainingDataset> findAllByOrderByCreatedAtDesc();
}
