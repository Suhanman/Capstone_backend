package com.emailagent.service.admin;

import com.emailagent.domain.entity.TrainedModel;
import com.emailagent.domain.enums.ModelStatus;
import com.emailagent.dto.response.admin.training.ModelActivateResponse;
import com.emailagent.dto.response.admin.training.TrainedModelDetailResponse;
import com.emailagent.dto.response.admin.training.TrainedModelListResponse;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.repository.TrainedModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainedModelServiceImpl implements TrainedModelService {

    private final TrainedModelRepository trainedModelRepository;

    @Override
    @Transactional(readOnly = true)
    public TrainedModelListResponse getModels() {
        List<TrainedModelListResponse.ModelSummary> summaries = trainedModelRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(TrainedModelListResponse.ModelSummary::from)
                .toList();
        return new TrainedModelListResponse(summaries);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainedModelDetailResponse getModel(Long modelId) {
        TrainedModel model = trainedModelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("모델을 찾을 수 없습니다: " + modelId));
        return new TrainedModelDetailResponse(model);
    }

    /**
     * 특정 모델을 운영용(ACTIVE)으로 전환.
     * 1. 전체 모델을 INACTIVE로 일괄 처리 (bulk UPDATE)
     * 2. 대상 모델만 ACTIVE로 전환
     * → 하나의 트랜잭션 내에서 처리되므로 동시에 두 개의 ACTIVE 모델이 존재하지 않음
     */
    @Override
    @Transactional
    public ModelActivateResponse activateModel(Long modelId) {
        // 1. 전체 INACTIVE 처리 (clearAutomatically = true 로 1차 캐시 초기화)
        trainedModelRepository.updateAllStatusTo(ModelStatus.INACTIVE);

        // 2. 대상 모델 조회 및 ACTIVE 전환
        TrainedModel model = trainedModelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("모델을 찾을 수 없습니다: " + modelId));
        model.activate();

        log.info("[TrainedModelService] 모델 ACTIVE 전환 — modelId={}, modelVersion={}",
                model.getModelId(), model.getModelVersion());

        return new ModelActivateResponse(model);
    }
}
