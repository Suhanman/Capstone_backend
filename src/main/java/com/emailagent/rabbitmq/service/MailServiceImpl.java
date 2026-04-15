package com.emailagent.rabbitmq.service;

import com.emailagent.domain.entity.Email;
import com.emailagent.domain.entity.EmailAnalysisResult;
import com.emailagent.domain.entity.Outbox;
import com.emailagent.domain.enums.OutboxStatus;
import com.emailagent.dto.response.admin.operation.AdminJobDetailResponse;
import com.emailagent.dto.response.admin.operation.AdminJobListResponse;
import com.emailagent.dto.response.admin.operation.AdminJobSummaryResponse;
import com.emailagent.rabbitmq.config.OutboxPolicy;
import com.emailagent.rabbitmq.dto.ClassifyResultDTO;
import com.emailagent.rabbitmq.event.SseNotifyEvent;
import com.emailagent.repository.EmailAnalysisResultRepository;
import com.emailagent.repository.EmailRepository;
import com.emailagent.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MailService 구현체.
 *
 * [상태 전이 책임]
 * READY → SENDING : markAsSendingBatch()
 * SENDING → READY  : markPublishedFailed() (브로커 전송 실패), resetTimedOut() (타임아웃)
 * SENDING → FINISH : markFinished() (AI 처리 성공)
 * SENDING → FAILED : markFailed() (재시도 3회 초과)
 *
 * [SSE 연동]
 * markFinished() 트랜잭션 커밋 후 SseNotifyEvent 발행 →
 * @TransactionalEventListener(AFTER_COMMIT)에서 SSE Pod 브로드캐스트 수행.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private static final int TIMEOUT_MINUTES = 30;

    private final OutboxRepository outboxRepository;
    private final EmailRepository emailRepository;
    private final EmailAnalysisResultRepository analysisResultRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ===================================================
    // 상태 전이 메서드
    // ===================================================

    @Override
    @Transactional
    public List<Outbox> markAsSendingBatch(int limit) {
        // [원자적 UPDATE 전략]
        // SELECT FOR UPDATE SKIP LOCKED + UPDATE를 한 번의 쿼리로 수행
        // → 로컬/Pod 환경 모두에서 동시성 충돌 방지
        //
        // Timeline:
        // 1. UPDATE 쿼리 내부의 SELECT FOR UPDATE로 READY 항목 lock
        // 2. 동시에 상태 UPDATE (READY → SENDING)
        // 3. lock이 유지된 상태에서 UPDATE 수행 → 다른 스레드/Pod 개입 불가
        // 4. findRecentlySending()으로 방금 UPDATE된 항목들 조회 및 반환

        int updatedCount = outboxRepository.updateReadyToSending(limit);
        if (updatedCount == 0) {
            return List.of();
        }

        // 방금 UPDATE된 항목들을 조회 (sent_at 기준으로 최근 1초)
        // → 경쟁 조건 없음 (이미 SENDING 상태로 확정됨)
        List<Outbox> sendingList = outboxRepository.findRecentlySending(limit);
        log.debug("[MailService] markAsSendingBatch: {}건 UPDATE, {}건 반환", updatedCount, sendingList.size());

        return sendingList;
    }

    @Override
    @Transactional
    public void markPublishedFailed(Long outboxId) {
        // Publisher Confirm ack=false → retryCount 증가 후 한도 초과 여부 판단
        // retry 판단 로직은 Service 계층이 담당 (Outbox 엔티티는 상태 전이만 수행)
        outboxRepository.findById(outboxId).ifPresent(outbox -> {
            outbox.incrementRetryCount();
            if (outbox.getRetryCount() >= OutboxPolicy.MAX_RETRY) {
                log.error("[MailService] 발행 최대 재시도 초과 → FAILED — outboxId={}, retryCount={}",
                        outboxId, outbox.getRetryCount());
                outbox.markAsFailed("Publisher Confirm ack=false, max retry exceeded");
            } else {
                log.warn("[MailService] Publisher Confirm 실패 → READY 롤백 — outboxId={}, retryCount={}",
                        outboxId, outbox.getRetryCount());
                outbox.markAsReady();
            }
        });
    }

    @Override
    @Transactional
    public void markFinished(ClassifyResultDTO result) {
        Long emailId = result.getEmailId();

        // 멱등성 보장: 이미 FINISH 처리된 건은 무시 (AI 서버 중복 발행 방어)
        Outbox outbox = outboxRepository.findById(result.getOutboxId())
                .orElseThrow(() -> new IllegalStateException("Outbox 없음: id=" + result.getOutboxId()));
        if (outbox.getStatus() == OutboxStatus.FINISH) {
            log.warn("[MailService] 이미 FINISH 처리된 outbox 수신 무시 — outboxId={}", result.getOutboxId());
            return;
        }

        // Outbox 완료 처리
        outbox.markAsFinished();

        // AI 분석 결과 저장 (upsert: 없으면 생성, 있으면 업데이트)
        Email email = emailRepository.findById(emailId)
                .orElseThrow(() -> new IllegalStateException("Email 없음: id=" + emailId));

        EmailAnalysisResult analysisResult = analysisResultRepository
                .findByEmail_EmailId(emailId)
                .orElseGet(() -> EmailAnalysisResult.builder().email(email).build());

        // List<Float> → float[] 변환
        float[] embedding = toFloatArray(result.getEmailEmbedding());

        // entities_json: AI가 JSON 문자열로 전송 → Map으로 역직렬화
        Map<String, Object> entitiesMap = null;
        String entitiesJsonStr = result.getEntitiesJson();
        if (entitiesJsonStr != null && !entitiesJsonStr.isBlank()) {
            try {
                entitiesMap = new ObjectMapper().readValue(
                        entitiesJsonStr, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("[MailService] entities_json 파싱 실패 — outboxId={}, raw={}",
                        result.getOutboxId(), entitiesJsonStr, e);
            }
        }

        analysisResult.updateFromClassify(
                result.getDomain(),
                result.getIntent(),
                result.getConfidenceScore(),
                result.getSummaryText(),
                result.isScheduleDetected(),
                embedding,
                entitiesMap,
                result.getModelVersion()
        );
        analysisResultRepository.save(analysisResult);

        log.info("[MailService] classify 완료 — outboxId={}, emailId={}", result.getOutboxId(), emailId);

        // 트랜잭션 커밋 후 SSE 브로드캐스트 발화
        eventPublisher.publishEvent(new SseNotifyEvent(this, emailId));
    }

    @Override
    @Transactional
    public void markFailed(Long outboxId, String reason) {
        outboxRepository.findById(outboxId).ifPresent(outbox -> {
            outbox.markAsFailed(reason);
            log.error("[MailService] Outbox FAILED — outboxId={}, reason={}", outboxId, reason);
        });
    }

    @Override
    @Transactional
    public void resetTimedOut() {
        // SENDING 상태가 30분 초과한 Outbox를 READY로 단순 롤백
        // retryCount를 소모하지 않음: 타임아웃은 발행 실패가 아닌 네트워크 지연 등 외부 요인이므로
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        List<Outbox> timedOut = outboxRepository.findTimedOutMessages(timeout);
        timedOut.forEach(outbox -> {
            log.warn("[MailService] SENDING 타임아웃 → READY 롤백 — outboxId={}", outbox.getOutboxId());
            outbox.markAsReady();
        });
    }

    // ===================================================
    // 관리자 Job 조회
    // ===================================================

    @Override
    @Transactional(readOnly = true)
    public AdminJobListResponse getJobList(String status, Pageable pageable) {
        Page<Outbox> page = (status == null || status.isBlank())
                ? outboxRepository.findAllWithEmailOrderByCreatedAtDesc(pageable)
                : outboxRepository.findByStatusWithEmailOrderByCreatedAtDesc(
                        OutboxStatus.valueOf(status.toUpperCase()), pageable);

        List<AdminJobListResponse.JobItem> items = page.getContent().stream()
                .map(o -> new AdminJobListResponse.JobItem(
                        o.getOutboxId(),
                        o.getEmail().getEmailId(),
                        o.getStatus().name(),
                        o.getCreatedAt().toString()))
                .toList();

        return new AdminJobListResponse(page.getTotalElements(), items);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminJobSummaryResponse getJobSummary() {
        return new AdminJobSummaryResponse(
                outboxRepository.countByStatus(OutboxStatus.READY),
                outboxRepository.countByStatus(OutboxStatus.FINISH),
                outboxRepository.countByStatus(OutboxStatus.FAILED)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminJobDetailResponse getJobDetail(Long outboxId) {
        Outbox outbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalArgumentException("Job 없음: id=" + outboxId));
        return new AdminJobDetailResponse(
                outbox.getOutboxId(),
                outbox.getEmail().getEmailId(),
                outbox.getStatus().name(),
                outbox.getPayload() != null ? outbox.getPayload().toString() : "",
                outbox.getRetryCount(),
                OutboxPolicy.MAX_RETRY,
                outbox.getCreatedAt().toString(),
                outbox.getSentAt() != null ? outbox.getSentAt().toString() : null,
                outbox.getFinishedAt() != null ? outbox.getFinishedAt().toString() : null
        );
    }

    @Override
    @Transactional
    public void forceDelete(Long outboxId) {
        Outbox outbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalArgumentException("Job 없음: id=" + outboxId));
        outboxRepository.delete(outbox);
        log.warn("[MailService] 관리자 강제 삭제 — outboxId={}", outboxId);
    }

    // ===================================================
    // 내부 유틸
    // ===================================================

    private float[] toFloatArray(List<Float> list) {
        if (list == null || list.isEmpty()) return new float[0];
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
}
