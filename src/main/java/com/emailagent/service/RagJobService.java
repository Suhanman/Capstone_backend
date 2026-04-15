package com.emailagent.service;

import com.emailagent.domain.entity.RagJob;
import com.emailagent.domain.entity.User;
import com.emailagent.domain.enums.RagJobStatus;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.rabbitmq.dto.RagDraftGenerateRequestDTO;
import com.emailagent.rabbitmq.dto.RagKnowledgeIngestRequestDTO;
import com.emailagent.rabbitmq.dto.RagKnowledgeIngestResultDTO;
import com.emailagent.rabbitmq.dto.RagProgressEventDTO;
import com.emailagent.rabbitmq.dto.RagTemplateIndexResultDTO;
import com.emailagent.repository.RagJobRepository;
import com.emailagent.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagJobService {

    private final RagJobRepository ragJobRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void createKnowledgeIngestJob(RagKnowledgeIngestRequestDTO request) {
        upsertQueuedJob(
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
    }

    @Transactional
    public void createDraftGenerationJob(RagDraftGenerateRequestDTO request) {
        String targetId = request.getCategoryId() != null ? String.valueOf(request.getCategoryId()) : null;
        upsertQueuedJob(
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
    }

    @Transactional
    public void completeKnowledgeIngest(RagKnowledgeIngestResultDTO result) {
        RagJob job = getExisting(result.getJobId());
        String payloadJson = toJson(result.getPayload());

        if ("SUCCESS".equalsIgnoreCase(result.getStatus())) {
            job.markCompleted("INDEXED", "지식 적재가 완료되었습니다.", payloadJson);
        } else {
            String errorCode = result.getError() != null ? result.getError().getCode() : null;
            String errorMessage = result.getError() != null ? result.getError().getMessage() : null;
            job.markFailed("FAILED", "지식 적재에 실패했습니다.", errorCode, errorMessage, payloadJson);
        }
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
    }

    private void upsertQueuedJob(
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
                .orElseGet(() -> ragJobRepository.save(
                        RagJob.builder()
                                .jobId(jobId)
                                .user(resolveUser(userId))
                                .requestId(requestId)
                                .jobType(jobType != null ? jobType : "rag")
                                .targetType(targetType)
                                .targetId(targetId)
                                .status(RagJobStatus.QUEUED)
                                .build()
                ));
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
}
