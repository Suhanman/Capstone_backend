package com.emailagent.repository;

import com.emailagent.domain.entity.TrainedModel;
import com.emailagent.domain.enums.ModelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrainedModelRepository extends JpaRepository<TrainedModel, Long> {

    /** 생성 시각 최신순 전체 목록 */
    List<TrainedModel> findAllByOrderByCreatedAtDesc();

    /** PATCH activate 시: 전체 모델을 먼저 INACTIVE 처리 */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE TrainedModel t SET t.status = :status")
    void updateAllStatusTo(@Param("status") ModelStatus status);

    /** model_version 중복 체크 (완료 이벤트 멱등성 보장) */
    boolean existsByModelVersion(String modelVersion);
}
