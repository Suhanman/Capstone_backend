package com.emailagent.controller.admin;

import com.emailagent.dto.request.admin.training.TrainingJobCreateRequest;
import com.emailagent.dto.response.admin.training.DatasetCollectionCreateResponse;
import com.emailagent.dto.response.admin.training.DatasetListResponse;
import com.emailagent.dto.response.admin.training.ModelActivateResponse;
import com.emailagent.dto.response.admin.training.TrainedModelDetailResponse;
import com.emailagent.dto.response.admin.training.TrainedModelListResponse;
import com.emailagent.dto.response.admin.training.TrainingJobCreateResponse;
import com.emailagent.dto.response.admin.training.TrainingJobDetailResponse;
import com.emailagent.security.CurrentUser;
import com.emailagent.service.admin.AiTrainingService;
import com.emailagent.service.admin.DatasetService;
import com.emailagent.service.admin.TrainedModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ai-training")
@RequiredArgsConstructor
public class AiTrainingController {

    private final AiTrainingService aiTrainingService;
    private final TrainedModelService trainedModelService;
    private final DatasetService datasetService;

    /**
     * POST /api/admin/ai-training/training-jobs
     * AI 학습 Job 생성 — training_jobs insert + q.2ai.training 발행
     */
    @PostMapping("/training-jobs")
    public ResponseEntity<TrainingJobCreateResponse> createTrainingJob(
            @CurrentUser Long userId,
            @Valid @RequestBody TrainingJobCreateRequest request) {
        return ResponseEntity.ok(aiTrainingService.createTrainingJob(userId, request));
    }

    /**
     * GET /api/admin/ai-training/jobs/{job_id}
     * Job 상태 및 결과 조회
     */
    @GetMapping("/jobs/{job_id}")
    public ResponseEntity<TrainingJobDetailResponse> getJobDetail(
            @PathVariable("job_id") String jobId) {
        return ResponseEntity.ok(aiTrainingService.getTrainingJob(jobId));
    }

    // ============================================================
    // 데이터셋 관리
    // ============================================================

    /**
     * GET /api/admin/ai-training/datasets
     * 등록된 데이터셋 버전, 건수, 상태 목록 조회 (최신순)
     */
    @GetMapping("/datasets")
    public ResponseEntity<DatasetListResponse> getDatasets() {
        return ResponseEntity.ok(datasetService.getDatasets());
    }

    /**
     * POST /api/admin/ai-training/dataset-collections
     * 원천 데이터(Email) 기반 학습용 데이터셋 버전 생성.
     * MQ 없이 Backend 단독 처리 — 현재 이메일 건수를 스냅샷으로 기록하고 버전 발급.
     */
    @PostMapping("/dataset-collections")
    public ResponseEntity<DatasetCollectionCreateResponse> createDatasetCollection() {
        return ResponseEntity.ok(datasetService.createDatasetCollection());
    }

    // ============================================================
    // 전처리 / Pair / 평가 Job (training_jobs 테이블 + MQ 재사용)
    // ============================================================

    /**
     * POST /api/admin/ai-training/preprocessing-jobs
     * 전처리 Job 생성 — 결측 제거, 공백 정리, email_text 생성 작업
     */
    @PostMapping("/preprocessing-jobs")
    public ResponseEntity<TrainingJobCreateResponse> createPreprocessingJob(
            @CurrentUser Long userId,
            @Valid @RequestBody TrainingJobCreateRequest request) {
        return ResponseEntity.ok(aiTrainingService.createPreprocessingJob(userId, request));
    }

    /**
     * POST /api/admin/ai-training/pair-jobs
     * Pair 생성 Job — SBERT 파인튜닝용 pair 데이터 생성 작업
     */
    @PostMapping("/pair-jobs")
    public ResponseEntity<TrainingJobCreateResponse> createPairJob(
            @CurrentUser Long userId,
            @Valid @RequestBody TrainingJobCreateRequest request) {
        return ResponseEntity.ok(aiTrainingService.createPairJob(userId, request));
    }

    /**
     * POST /api/admin/ai-training/evaluation-jobs
     * 평가 Job 생성 — 학습 완료 모델 성능 평가 작업
     */
    @PostMapping("/evaluation-jobs")
    public ResponseEntity<TrainingJobCreateResponse> createEvaluationJob(
            @CurrentUser Long userId,
            @Valid @RequestBody TrainingJobCreateRequest request) {
        return ResponseEntity.ok(aiTrainingService.createEvaluationJob(userId, request));
    }

    // ============================================================
    // 모델 관리
    // ============================================================

    /**
     * GET /api/admin/ai-training/models
     * 저장된 모델 버전 목록 및 성능 요약 조회 (최신순)
     */
    @GetMapping("/models")
    public ResponseEntity<TrainedModelListResponse> getModels() {
        return ResponseEntity.ok(trainedModelService.getModels());
    }

    /**
     * GET /api/admin/ai-training/models/{model_id}
     * 모델 상세 정보 조회
     */
    @GetMapping("/models/{model_id}")
    public ResponseEntity<TrainedModelDetailResponse> getModel(
            @PathVariable("model_id") Long modelId) {
        return ResponseEntity.ok(trainedModelService.getModel(modelId));
    }

    /**
     * PATCH /api/admin/ai-training/models/{model_id}
     * 특정 모델을 운영용(ACTIVE)으로 전환 — 기존 ACTIVE 모델은 자동 INACTIVE 처리
     */
    @PatchMapping("/models/{model_id}")
    public ResponseEntity<ModelActivateResponse> activateModel(
            @PathVariable("model_id") Long modelId) {
        return ResponseEntity.ok(trainedModelService.activateModel(modelId));
    }
}
