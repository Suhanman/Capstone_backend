package com.emailagent.messaging;

import com.emailagent.config.RabbitMQConfig;
import com.emailagent.domain.entity.CalendarEvent;
import com.emailagent.domain.entity.Email;
import com.emailagent.domain.entity.EmailAnalysisResult;
import com.emailagent.repository.CalendarEventRepository;
import com.emailagent.repository.EmailAnalysisResultRepository;
import com.emailagent.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailClassifyConsumer {

    private final EmailRepository emailRepository;
    private final EmailAnalysisResultRepository analysisResultRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final EmailMessagePublisher emailMessagePublisher;

    /**
     * AI 분류 결과 수신: email.classify 큐
     * 수신 메시지 구조:
     * {
     *   "request_id": "req_001",
     *   "emailId": 123,
     *   "classification": {
     *     "domain": "Finance",
     *     "intent": "세금계산서 요청",
     *     "domain_confidence": 0.91,
     *     "intent_confidence": 0.95
     *   },
     *   "summary": "세금계산서 발행 요청 메일",
     *   "schedule_info": {
     *     "has_schedule": false,
     *     "title": null,
     *     "date": null,
     *     "time": null,
     *     "location": null
     *   },
     *   "email_embedding": [0.12, -0.03, ...] // 384차원 float 배열
     * }
     */
    @RabbitListener(queues = RabbitMQConfig.CLASSIFY_QUEUE)
    @Transactional
    public void receiveClassifyResult(Map<String, Object> message) {
        Long emailId = Long.valueOf(message.get("emailId").toString());
        String requestId = message.get("request_id").toString();

        log.info("AI 분류 결과 수신: emailId={}, requestId={}", emailId, requestId);

        try {
            // classification 중첩 객체 파싱
            @SuppressWarnings("unchecked")
            Map<String, Object> classification = (Map<String, Object>) message.get("classification");
            String domain = classification.get("domain").toString();
            String intent = classification.get("intent").toString();
            // domain_confidence를 confidence_score로 사용
            BigDecimal confidenceScore = new BigDecimal(classification.get("domain_confidence").toString());

            String summary = message.get("summary") != null ? message.get("summary").toString() : "";

            // schedule_info 파싱
            @SuppressWarnings("unchecked")
            Map<String, Object> scheduleInfo = (Map<String, Object>) message.get("schedule_info");
            boolean hasSchedule = scheduleInfo != null
                    && Boolean.parseBoolean(scheduleInfo.get("has_schedule").toString());

            // email_embedding: List<Double> → float[] 변환 (VectorConverter가 VECTOR 바이너리로 저장)
            float[] emailEmbedding = null;
            if (message.get("email_embedding") != null) {
                @SuppressWarnings("unchecked")
                java.util.List<Number> embeddingList = (java.util.List<Number>) message.get("email_embedding");
                emailEmbedding = new float[embeddingList.size()];
                for (int i = 0; i < embeddingList.size(); i++) {
                    emailEmbedding[i] = embeddingList.get(i).floatValue();
                }
            }

            // 1. EmailAnalysisResults 조회 또는 신규 생성 후 분류 결과 저장
            Email email = emailRepository.getReferenceById(emailId);
            EmailAnalysisResult analysisResult = analysisResultRepository
                    .findByEmail_EmailId(emailId)
                    .orElseGet(() -> EmailAnalysisResult.builder().email(email).build());

            analysisResult.updateFromClassify(domain, intent, confidenceScore, summary,
                    hasSchedule, emailEmbedding);
            analysisResultRepository.save(analysisResult);

            // 2. 일정 감지 시 CalendarEvents에 PENDING 상태로 저장
            if (hasSchedule) {
                saveCalendarEvent(emailId, email, scheduleInfo);
            }

            // 3. 분류 완료 후 초안 생성 요청 발행 (mode="generate")
            // subject, body는 Email 엔티티에서 가져옴 (getReferenceById → 실제 조회 필요)
            Email emailEntity = emailRepository.findById(emailId)
                    .orElseThrow(() -> new IllegalStateException("이메일을 찾을 수 없습니다: " + emailId));

            emailMessagePublisher.publishDraftRequest(
                    emailId,
                    emailEntity.getSubject(),
                    emailEntity.getBodyClean(),
                    domain,
                    intent,
                    summary,
                    null,   // mailTone: 현재 단계에서 미확정
                    null,   // ragContext: 현재 단계에서 미확정
                    null,   // previousDraft: generate 모드이므로 null
                    "generate"
            );

            log.info("분류 처리 완료: emailId={}, domain={}, intent={}", emailId, domain, intent);

        } catch (Exception e) {
            log.error("분류 결과 처리 실패: emailId={}, error={}", emailId, e.getMessage(), e);
        }
    }

    /**
     * schedule_info 기반 CalendarEvent 저장
     * date + time → LocalDateTime 파싱, 없으면 당일 00:00 기본값 사용
     */
    private void saveCalendarEvent(Long emailId, Email email, Map<String, Object> scheduleInfo) {
        try {
            String title = scheduleInfo.get("title") != null
                    ? scheduleInfo.get("title").toString() : "일정";

            String dateStr = scheduleInfo.get("date") != null
                    ? scheduleInfo.get("date").toString() : null;
            String timeStr = scheduleInfo.get("time") != null
                    ? scheduleInfo.get("time").toString() : null;

            if (dateStr == null) {
                log.warn("schedule_info에 date 없음, CalendarEvent 저장 건너뜀: emailId={}", emailId);
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            // time이 없으면 자정(00:00)으로 기본값 설정
            LocalTime time = (timeStr != null) ? LocalTime.parse(timeStr) : LocalTime.MIDNIGHT;
            LocalDateTime startDatetime = LocalDateTime.of(date, time);

            CalendarEvent event = CalendarEvent.builder()
                    .user(email.getUser())
                    .email(email)
                    .title(title)
                    .startDatetime(startDatetime)
                    .source("EMAIL")
                    .status("PENDING")
                    .build();

            calendarEventRepository.save(event);
            log.info("CalendarEvent 저장 완료: emailId={}, title={}, startDatetime={}", emailId, title, startDatetime);

        } catch (Exception e) {
            log.error("CalendarEvent 저장 실패: emailId={}, error={}", emailId, e.getMessage());
        }
    }
}
