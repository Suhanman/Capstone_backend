package com.emailagent.service;

import com.emailagent.domain.entity.RagJob;
import com.emailagent.domain.entity.User;
import com.emailagent.domain.enums.RagJobStatus;
import com.emailagent.dto.response.onboarding.OnboardingTemplateJobStatusResponse;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.rabbitmq.dto.RagDraftGenerateRequestDTO;
import com.emailagent.rabbitmq.dto.RagDraftGenerateResultDTO;
import com.emailagent.rabbitmq.dto.RagKnowledgeIngestRequestDTO;
import com.emailagent.rabbitmq.dto.RagKnowledgeIngestResultDTO;
import com.emailagent.rabbitmq.dto.RagProgressEventDTO;
import com.emailagent.rabbitmq.dto.RagTemplateMatchRequestDTO;
import com.emailagent.rabbitmq.dto.RagTemplateMatchResultDTO;
import com.emailagent.rabbitmq.dto.RagTemplateIndexResultDTO;
import com.emailagent.repository.RagJobRepository;
import com.emailagent.repository.UserRepository;
import com.emailagent.sse.service.SseEmitterService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagJobService {

    private final RagJobRepository ragJobRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final SseEmitterService sseEmitterService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createKnowledgeIngestJob(RagKnowledgeIngestRequestDTO request) {
        RagJob job = upsertQueuedJob(
                request.getJobId(),
                request.getRequestId(),
                request.getUserId(),
                "knowledge.ingest",
                "knowledge",
                null,
                "QUEUED",
                "지식 적재 요청 대기 중",
                request
        );
        pushJobUpdate(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createDraftGenerationJob(RagDraftGenerateRequestDTO request) {
        String targetId = request.getCategoryId() != null ? String.valueOf(request.getCategoryId()) : null;
        RagJob job = upsertQueuedJob(
                request.getJobId(),
                request.getRequestId(),
                request.getUserId(),
                "draft.generate",
                "category",
                targetId,
                "QUEUED",
                "템플릿 생성 요청 대기 중",
                request
        );
        pushJobUpdate(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createTemplateMatchJob(RagTemplateMatchRequestDTO request) {
        String targetId = request.getPayload() != null ? request.getPayload().getEmailId() : null;
        RagJob job = upsertQueuedJob(
                request.getJobId(),
                request.getRequestId(),
                request.getUserId(),
                "templates.match",
                "email",
                targetId,
                "QUEUED",
                "템플릿 추천 요청 대기 중",
                request
        );
        pushJobUpdate(job);
    }

    @Transactional
    public void applyProgress(RagProgressEventDTO progress) {
        RagJob job = getOrCreate(progress.getJobId(), progress.getUserId(), progress.getRequestId(), progress.getJobType(), progress.getTargetType(), progress.getTargetId());
        String payloadJson = toJson(progress.getPayload());

        if ("FAILED".equalsIgnoreCase(progress.getStatus())) {
            String errorCode = progress.getError() != null ? progress.getError().getCode() : null;
            String errorMessage = progress.getError() != null ? progress.getError().getMessage() : null;
            job.markFailed(progress.getProgressStep(), progress.getProgressMessage(), errorCode, errorMessage, payloadJson);
        } else if ("COMPLETED".equalsIgnoreCase(progress.getStatus())) {
            job.markCompleted(progress.getProgressStep(), progress.getProgressMessage(), payloadJson);
        } else if ("QUEUED".equalsIgnoreCase(progress.getStatus())) {
            job.markQueued(progress.getProgressStep(), progress.getProgressMessage(), payloadJson);
        } else {
            job.markProcessing(progress.getProgressStep(), progress.getProgressMessage(), payloadJson);
        }
        pushJobUpdate(job);
    }

    @Transactional
    public void completeKnowledgeIngest(RagKnowledgeIngestResultDTO result) {
        RagJob job = getOrCreate(result.getJobId(), result.getUserId(), result.getRequestId(), "knowledge.ingest", "knowledge", null);
        String payloadJson = toJson(result.getPayload());

        if ("SUCCESS".equalsIgnoreCase(result.getStatus())) {
            job.markCompleted("INDEXED", "지식 적재가 완료되었습니다.", payloadJson);
        } else {
            String errorCode = result.getError() != null ? result.getError().getCode() : null;
            String errorMessage = result.getError() != null ? result.getError().getMessage() : null;
            job.markFailed("FAILED", "지식 적재에 실패했습니다.", errorCode, errorMessage, payloadJson);
        }
        pushJobUpdate(job);
    }

    @Transactional
    public void completeTemplateIndex(String requestId, Long userId, RagTemplateIndexResultDTO result) {
        String jobId = "template-index-" + userId + "-" + requestId;
        RagJob job = getOrCreate(jobId, userId, requestId, "templates.index", "template", null);
        String payloadJson = toJson(result.getPayload());

        if ("SUCCESS".equalsIgnoreCase(result.getStatus())) {
            job.markCompleted("INDEXED", "템플릿 인덱싱이 완료되었습니다.", payloadJson);
        } else {
            String errorCode = result.getError() != null ? result.getError().getCode() : null;
            String errorMessage = result.getError() != null ? result.getError().getMessage() : null;
            job.markFailed("FAILED", "템플릿 인덱싱에 실패했습니다.", errorCode, errorMessage, payloadJson);
        }
        pushJobUpdate(job);
    }

    @Transactional
    public void completeTemplateMatch(RagTemplateMatchResultDTO result) {
        String targetId = result.getPayload() != null ? result.getPayload().getEmailId() : null;
        RagJob job = getOrCreate(result.getJobId(), result.getUserId(), result.getRequestId(), "templates.match", "email", targetId);
        String payloadJson = toJson(result.getPayload());

        if ("SUCCESS".equalsIgnoreCase(result.getStatus())) {
            job.markCompleted("MATCHED", "추천 템플릿 조회가 완료되었습니다.", payloadJson);
        } else {
            String errorCode = result.getError() != null ? result.getError().getCode() : null;
            String errorMessage = result.getError() != null ? result.getError().getMessage() : null;
            job.markFailed("FAILED", "추천 템플릿 조회에 실패했습니다.", errorCode, errorMessage, payloadJson);
        }
        pushJobUpdate(job);
    }

    @Transactional
    public void completeDraftGeneration(RagDraftGenerateResultDTO result) {
        String targetId = result.getPayload() != null && result.getPayload().getCategoryId() != null
                ? String.valueOf(result.getPayload().getCategoryId())
                : null;
        RagJob job = getOrCreate(result.getJobId(), result.getUserId(), result.getRequestId(), "draft.generate", "category", targetId);
        String payloadJson = toJson(result.getPayload());

        if ("SUCCESS".equalsIgnoreCase(result.getStatus())) {
            job.markCompleted("GENERATED", "카테고리별 맞춤 템플릿 생성이 완료되었습니다.", payloadJson);
        } else {
            String errorCode = result.getError() != null ? result.getError().getCode() : null;
            String errorMessage = result.getError() != null ? result.getError().getMessage() : null;
            job.markFailed("FAILED", "템플릿 생성에 실패했습니다.", errorCode, errorMessage, payloadJson);
        }
        pushJobUpdate(job);
    }

    @Transactional(readOnly = true)
    public OnboardingTemplateJobStatusResponse getTemplateGenerationJobs(Long userId, List<String> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            return OnboardingTemplateJobStatusResponse.of(List.of());
        }

        List<RagJob> jobs = ragJobRepository.findByUser_UserIdAndJobIdInOrderByCreatedAtAsc(userId, jobIds);
        return OnboardingTemplateJobStatusResponse.of(jobs);
    }

    private RagJob upsertQueuedJob(
            String jobId,
            String requestId,
            Long userId,
            String jobType,
            String targetType,
            String targetId,
            String progressStep,
            String progressMessage,
            Object payload
    ) {
        RagJob job = getOrCreate(jobId, userId, requestId, jobType, targetType, targetId);
        job.markQueued(progressStep, progressMessage, toJson(payload));
        return job;
    }

    private RagJob getExisting(String jobId) {
        return ragJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("RAG Job을 찾을 수 없습니다: " + jobId));
    }

    private RagJob getOrCreate(
            String jobId,
            Long userId,
            String requestId,
            String jobType,
            String targetType,
            String targetId
    ) {
        return ragJobRepository.findById(jobId)
                .orElseGet(() -> {
                    try {
                        return ragJobRepository.saveAndFlush(
                                RagJob.builder()
                                        .jobId(jobId)
                                        .user(resolveUser(userId))
                                        .requestId(requestId)
                                        .jobType(jobType != null ? jobType : "rag")
                                        .targetType(targetType)
                                        .targetId(targetId)
                                        .status(RagJobStatus.QUEUED)
                                        .build()
                        );
                    } catch (DataIntegrityViolationException error) {
                        log.info("[RagJobService] job 생성 경합 감지 — 기존 row 재조회: jobId={}", jobId);
                        return ragJobRepository.findById(jobId)
                                .orElseThrow(() -> error);
                    }
                });
    }

    private User resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    }

    private String toJson(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("[RagJobService] payload JSON 직렬화 실패: {}", e.getMessage());
            return null;
        }
    }

    private void pushJobUpdate(RagJob job) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("job_id", job.getJobId());
        payload.put("request_id", job.getRequestId());
        payload.put("job_type", job.getJobType());
        payload.put("target_type", job.getTargetType());
        payload.put("target_id", job.getTargetId());
        payload.put("status", job.getStatus().name());
        payload.put("progress_step", job.getProgressStep());
        payload.put("progress_message", job.getProgressMessage());
        payload.put("error_code", job.getErrorCode());
        payload.put("error_message", job.getErrorMessage());
        payload.put("completed_at", job.getCompletedAt());

        sseEmitterService.sendEventToUser(
                job.getUser().getUserId(),
                "rag-job-updated",
                payload
        );
    }
}
