package com.emailagent.messaging;

import com.emailagent.domain.entity.EmailAnalysisResult;
import com.emailagent.domain.entity.Outbox;
import com.emailagent.domain.enums.OutboxStatus;
import com.emailagent.repository.EmailAnalysisResultRepository;
import com.emailagent.repository.EmailRepository;
import com.emailagent.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailAnalysisConsumer {

    private final EmailRepository emailRepository;
    private final EmailAnalysisResultRepository analysisResultRepository;
    private final OutboxRepository outboxRepository;

    /**
     * AI 서버의 분석 결과 수신
     * 응답 예시:
     * {
     *   "email_id": 1,
     *   "outbox_id": 1,
     *   "domain": "마케팅",
     *   "intent": "광고 문의",
     *   "confidence": 0.94,
     *   "summary": "ABC 마케팅에서 SNS 광고 협업 문의",
     *   "entities": {"customer_name": "홍길동", "company": "ABC Corp"},
     *   "schedule_detected": false
     * }
     */
    @RabbitListener(queues = "${app.rabbitmq.queue.response}")
    @Transactional
    public void receiveAnalysisResult(Map<String, Object> result) {
        Long emailId = Long.valueOf(result.get("email_id").toString());
        Long outboxId = Long.valueOf(result.get("outbox_id").toString());

        log.info("AI 분석 결과 수신: emailId={}, domain={}, intent={}",
                emailId, result.get("domain"), result.get("intent"));

        try {
            // 1. 분석 결과 저장 (없으면 신규 생성)
            EmailAnalysisResult analysisResult = analysisResultRepository
                    .findByEmail_EmailId(emailId)
                    .orElseGet(() -> {
                        return EmailAnalysisResult.builder()
                                .email(emailRepository.getReferenceById(emailId))
                                .build();
                    });

            analysisResult.updateAnalysisResult(
                    result.get("domain").toString(),
                    result.get("intent").toString(),
                    new BigDecimal(result.get("confidence").toString()),
                    result.get("summary").toString(),
                    (Map<String, Object>) result.get("entities"),
                    Boolean.parseBoolean(result.get("schedule_detected").toString())
            );
            analysisResultRepository.save(analysisResult);

            // 2. Outbox 완료 처리
            outboxRepository.findById(outboxId).ifPresent(outbox -> {
                outbox.markAsFinished();
                outboxRepository.save(outbox);
            });

        } catch (Exception e) {
            log.error("분석 결과 처리 실패: emailId={}, error={}", emailId, e.getMessage());
            outboxRepository.findById(outboxId).ifPresent(outbox -> {
                outbox.markAsFailed(e.getMessage());
                outboxRepository.save(outbox);
            });
        }
    }
}
