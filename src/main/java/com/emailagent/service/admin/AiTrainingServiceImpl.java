package com.emailagent.service.admin;

import com.emailagent.domain.entity.TrainedModel;
import com.emailagent.domain.entity.TrainingJob;
import com.emailagent.domain.enums.TrainingJobStatus;
import com.emailagent.dto.request.admin.training.TrainingJobCreateRequest;
import com.emailagent.dto.response.admin.training.TrainingJobCreateResponse;
import com.emailagent.dto.response.admin.training.TrainingJobDetailResponse;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.kubernetes.KubernetesJobTrigger;
import com.emailagent.rabbitmq.dto.TrainingJobResultMessage;
import com.emailagent.repository.TrainedModelRepository;
import com.emailagent.repository.TrainingJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTrainingServiceImpl implements AiTrainingService {

    private final TrainingJobRepository trainingJobRepository;
    private final TrainedModelRepository trainedModelRepository;
    private final KubernetesJobTrigger kubernetesJobTrigger;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public TrainingJobCreateResponse createTrainingJob(Long adminUserId, TrainingJobCreateRequest request) {
        return createJobInternal(adminUserId, request.getDatasetVersion(), "training", "training");
    }

    @Override
    @Transactional
    public TrainingJobCreateResponse createPreprocessingJob(Long adminUserId, TrainingJobCreateRequest request) {
        return createJobInternal(adminUserId, request.getDatasetVersion(), "preprocessing", "preprocessing");
    }

    @Override
    @Transactional
    public TrainingJobCreateResponse createPairJob(Long adminUserId, TrainingJobCreateRequest request) {
        return createJobInternal(adminUserId, request.getDatasetVersion(), "pair", "pair");
    }

    @Override
    @Transactional
    public TrainingJobCreateResponse createEvaluationJob(Long adminUserId, TrainingJobCreateRequest request) {
        return createJobInternal(adminUserId, request.getDatasetVersion(), "evaluation", "evaluation");
    }

    /**
     * Job 생성 공통 로직.
     *
     * [처리 흐름]
     * 1. UUID job_id 생성 → training_jobs insert (QUEUED)
     * 2. Kubernetes API 호출 → Go 컨테이너 k8s Job 생성
     *    - Go Job이 데이터 가공 / S3 업로드 / AI 학습 트리거를 담당
     *    - job_id, job_type, dataset_version 등을 환경변수로 전달
     *
     * [k8s 트리거 실패 시]
     * 예외를 그대로 전파 → @Transactional 롤백 → DB에 orphan row 남지 않음
     * 클라이언트는 500 응답을 받고 재시도 가능
     */
    private TrainingJobCreateResponse createJobInternal(Long adminUserId, String datasetVersion,
                                                        String jobType, String taskType) {
        String jobId = UUID.randomUUID().toString();
        String requestedBy = String.valueOf(adminUserId);
        String createdAt = Instant.now().toString();

        // 1. training_jobs 테이블 insert (QUEUED)
        TrainingJob job = TrainingJob.builder()
                .jobId(jobId)
                .jobType(jobType)
                .taskType(taskType)
                .datasetVersion(datasetVersion)
                .requestedBy(requestedBy)
                .build();
        trainingJobRepository.save(job);

        // 2. k8s Job 트리거 — 실패 시 예외 전파로 트랜잭션 롤백
        try {
            kubernetesJobTrigger.trigger(jobId, jobType, taskType, datasetVersion, requestedBy);
        } catch (Exception e) {
            log.error("[AiTrainingService] k8s Job 생성 실패 — jobId={}, jobType={}, error={}",
                    jobId, jobType, e.getMessage(), e);
            throw new RuntimeException("k8s Job 생성에 실패했습니다. jobId=" + jobId, e);
        }

        log.info("[AiTrainingService] Job 생성 완료 — jobType={}, jobId={}, datasetVersion={}",
                jobType, jobId, datasetVersion);

        return new TrainingJobCreateResponse(
                jobId,
                TrainingJobStatus.QUEUED.name(),
                datasetVersion,
                createdAt
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingJobDetailResponse getTrainingJob(String jobId) {
        TrainingJob job = trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("학습 Job을 찾을 수 없습니다: " + jobId));
        return new TrainingJobDetailResponse(job);
    }

    /**
     * AI worker 완료 이벤트 처리 (기존 그대로 유지).
     * q.2app.training 에서 수신한 결과로 training_jobs 상태 업데이트.
     * status "completed" → COMPLETED + trained_models 자동 등록
     * status 그 외   → FAILED
     */
    @Override
    @Transactional
    public void handleTrainingResult(TrainingJobResultMessage result) {
        TrainingJob job = trainingJobRepository.findById(result.getJobId())
                .orElseThrow(() -> new IllegalArgumentException("Job을 찾을 수 없습니다: " + result.getJobId()));

        LocalDateTime finishedAt = parseFinishedAt(result.getFinishedAt());

        if ("completed".equalsIgnoreCase(result.getStatus())) {
            String metricsJson = serializeMetrics(result);
            job.complete(result.getModelVersion(), metricsJson, finishedAt);
            registerTrainedModelIfAbsent(job.getJobId(), result, metricsJson);
            log.info("[AiTrainingService] Job COMPLETED — jobId={}, modelVersion={}",
                    job.getJobId(), result.getModelVersion());
        } else {
            job.fail(result.getErrorMessage(), finishedAt);
            log.warn("[AiTrainingService] Job FAILED — jobId={}, error={}",
                    job.getJobId(), result.getErrorMessage());
        }
    }

    /**
     * 학습 완료 후 trained_models 테이블에 모델 등록.
     * model_version 기준으로 중복 체크하여 멱등성 보장.
     */
    private void registerTrainedModelIfAbsent(String jobId, TrainingJobResultMessage result, String metricsJson) {
        String modelVersion = result.getModelVersion();
        if (modelVersion == null || trainedModelRepository.existsByModelVersion(modelVersion)) {
            log.debug("[AiTrainingService] trained_model 등록 건너뜀 — modelVersion={}", modelVersion);
            return;
        }

        TrainedModel model = TrainedModel.builder()
                .modelVersion(modelVersion)
                .jobId(jobId)
                .intentF1(extractDouble(result.getMetrics(), "intent_f1"))
                .domainAccuracy(extractDouble(result.getMetrics(), "domain_accuracy"))
                .metricsJson(metricsJson)
                .build();
        trainedModelRepository.save(model);

        log.info("[AiTrainingService] trained_model 등록 완료 — modelVersion={}", modelVersion);
    }

    private Double extractDouble(Map<String, Object> metrics, String key) {
        if (metrics == null || !metrics.containsKey(key)) return null;
        Object value = metrics.get(key);
        if (value instanceof Number num) return num.doubleValue();
        return null;
    }

    private LocalDateTime parseFinishedAt(String finishedAt) {
        if (finishedAt == null) return LocalDateTime.now();
        try {
            return Instant.parse(finishedAt).atZone(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException e) {
            log.warn("[AiTrainingService] finished_at 파싱 실패, 현재 시각으로 대체: {}", finishedAt);
            return LocalDateTime.now();
        }
    }

    private String serializeMetrics(TrainingJobResultMessage result) {
        if (result.getMetrics() == null) return null;
        try {
            return objectMapper.writeValueAsString(result.getMetrics());
        } catch (JsonProcessingException e) {
            log.warn("[AiTrainingService] metrics 직렬화 실패: {}", e.getMessage());
            return null;
        }
    }
}
