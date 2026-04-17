package com.emailagent.service.admin;

import com.emailagent.dto.response.admin.training.ModelActivateResponse;
import com.emailagent.dto.response.admin.training.TrainedModelDetailResponse;
import com.emailagent.dto.response.admin.training.TrainedModelListResponse;

public interface TrainedModelService {

    /** 전체 모델 목록 조회 (최신순) */
    TrainedModelListResponse getModels();

    /** 모델 상세 조회 */
    TrainedModelDetailResponse getModel(Long modelId);

    /** 특정 모델을 운영용(ACTIVE)으로 전환 — 기존 ACTIVE 모델은 자동 INACTIVE 처리 */
    ModelActivateResponse activateModel(Long modelId);
}
