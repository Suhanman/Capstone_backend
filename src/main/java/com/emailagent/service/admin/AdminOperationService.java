package com.emailagent.service.admin;

import com.emailagent.domain.entity.Outbox;
import com.emailagent.domain.enums.OutboxStatus;
import com.emailagent.dto.response.admin.operation.AdminJobDeleteResponse;
import com.emailagent.dto.response.admin.operation.AdminJobDetailResponse;
import com.emailagent.dto.response.admin.operation.AdminJobErrorResponse;
import com.emailagent.dto.response.admin.operation.AdminJobListResponse;
import com.emailagent.dto.response.admin.operation.AdminJobSummaryResponse;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOperationService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * 작업 목록 조회 (status 선택 필터, 페이징)
     * - 스펙 상태값: READY / SUCCESS / FAILED
     * - SUCCESS는 내부적으로 OutboxStatus.FINISH에 매핑
     */
    @Transactional(readOnly = true)
    public AdminJobListResponse getJobs(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size);

        Page<Outbox> outboxPage;
        if (status != null && !status.isBlank()) {
            OutboxStatus outboxStatus = resolveStatus(status);
            outboxPage = outboxRepository.findByStatusWithEmailOrderByCreatedAtDesc(outboxStatus, pageable);
        } else {
            outboxPage = outboxRepository.findAllWithEmailOrderByCreatedAtDesc(pageable);
        }

        List<AdminJobListResponse.JobItem> items = outboxPage.getContent().stream()
                .map(o -> new AdminJobListResponse.JobItem(
                        o.getOutboxId(),
                        o.getEmail().getEmailId(),
                        o.getStatus().name(),
                        o.getCreatedAt().toInstant(ZoneOffset.UTC).toString()
                ))
                .collect(Collectors.toList());

        return new AdminJobListResponse(outboxPage.getTotalElements(), items);
    }

    /**
     * 작업 상태별 건수 요약
     * - success_count = OutboxStatus.FINISH 건수
     */
    @Transactional(readOnly = true)
    public AdminJobSummaryResponse getJobsSummary() {
        long readyCount = outboxRepository.countByStatus(OutboxStatus.READY);
        long successCount = outboxRepository.countByStatus(OutboxStatus.FINISH);
        long failedCount = outboxRepository.countByStatus(OutboxStatus.FAILED);
        return new AdminJobSummaryResponse(readyCount, successCount, failedCount);
    }

    /**
     * 특정 작업 상세 조회
     * - payload(Map) → JSON 문자열로 직렬화하여 반환
     */
    @Transactional(readOnly = true)
    public AdminJobDetailResponse getJobDetail(Long jobId) {
        Outbox outbox = findOutboxById(jobId);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(outbox.getPayload());
        } catch (JsonProcessingException e) {
            payloadJson = "{}";
        }

        String sentAt = outbox.getSentAt() != null
                ? outbox.getSentAt().toInstant(ZoneOffset.UTC).toString() : null;
        String finishedAt = outbox.getFinishedAt() != null
                ? outbox.getFinishedAt().toInstant(ZoneOffset.UTC).toString() : null;

        return new AdminJobDetailResponse(
                outbox.getOutboxId(),
                outbox.getEmail().getEmailId(),
                outbox.getStatus().name(),
                payloadJson,
                outbox.getRetryCount(),
                outbox.getMaxRetry(),
                outbox.getCreatedAt().toInstant(ZoneOffset.UTC).toString(),
                sentAt,
                finishedAt
        );
    }

    /**
     * 특정 작업의 실패 원인 로그 조회
     * - FAILED 상태인 작업만 의미 있는 fail_reason을 가짐
     */
    @Transactional(readOnly = true)
    public AdminJobErrorResponse getJobError(Long jobId) {
        Outbox outbox = findOutboxById(jobId);

        if (outbox.getStatus() != OutboxStatus.FAILED) {
            throw new IllegalStateException("FAILED 상태인 작업만 실패 원인을 조회할 수 있습니다. outboxId=" + jobId);
        }

        return new AdminJobErrorResponse(outbox.getOutboxId(), outbox.getFailReason());
    }

    /**
     * 작업 강제 삭제
     * - READY / FAILED 상태인 작업만 삭제 허용
     * - SENDING / FINISH 상태는 파이프라인 진행 중이므로 삭제 거부
     */
    @Transactional
    public AdminJobDeleteResponse deleteJob(Long jobId) {
        Outbox outbox = findOutboxById(jobId);

        if (outbox.getStatus() != OutboxStatus.READY && outbox.getStatus() != OutboxStatus.FAILED) {
            throw new IllegalStateException(
                    "READY 또는 FAILED 상태인 작업만 삭제할 수 있습니다. 현재 상태: " + outbox.getStatus());
        }

        outboxRepository.delete(outbox);
        return new AdminJobDeleteResponse();
    }

    // ── private ──────────────────────────────────────────────────────────────

    private Outbox findOutboxById(Long jobId) {
        return outboxRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("작업을 찾을 수 없습니다. outboxId=" + jobId));
    }

    /**
     * 스펙 상태값(SUCCESS) → 내부 enum(FINISH) 매핑
     */
    private OutboxStatus resolveStatus(String status) {
        if ("SUCCESS".equalsIgnoreCase(status)) {
            return OutboxStatus.FINISH;
        }
        try {
            return OutboxStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 상태값입니다: " + status);
        }
    }
}
