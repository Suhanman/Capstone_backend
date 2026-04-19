package com.emailagent.service.admin;

import com.emailagent.dto.request.admin.training.TrainingJobCreateRequest;
import com.emailagent.dto.response.admin.training.TrainingJobCreateResponse;
import com.emailagent.dto.response.admin.training.TrainingJobDetailResponse;
import com.emailagent.rabbitmq.dto.TrainingJobResultMessage;

public interface AiTrainingService {

    /** AI 학습 Job 생성 — training_jobs insert + q.2ai.training 발행 */
    TrainingJobCreateResponse createTrainingJob(Long adminUserId, TrainingJobCreateRequest request);

    /** 전처리 Job 생성 (결측 제거, 공백 정리) — job_type="preprocessing" */
    TrainingJobCreateResponse createPreprocessingJob(Long adminUserId, TrainingJobCreateRequest request);

    /** Pair 생성 Job 생성 (SBERT 파인튜닝용) — job_type="pair" */
    TrainingJobCreateResponse createPairJob(Long adminUserId, TrainingJobCreateRequest request);

    /** 평가 Job 생성 (학습 완료 모델 성능 평가) — job_type="evaluation" */
    TrainingJobCreateResponse createEvaluationJob(Long adminUserId, TrainingJobCreateRequest request);

    /** Job 상태 및 결과 조회 */
    TrainingJobDetailResponse getTrainingJob(String jobId);

    /** AI worker 완료 이벤트 처리 — training_jobs 상태 업데이트 */
    void handleTrainingResult(TrainingJobResultMessage result);
}
