package com.emailagent.messaging;

import com.emailagent.config.RabbitMQConfig;
import com.emailagent.domain.entity.DraftReply;
import com.emailagent.domain.entity.Email;
import com.emailagent.domain.enums.DraftStatus;
import com.emailagent.domain.enums.EmailStatus;
import com.emailagent.repository.DraftReplyRepository;
import com.emailagent.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailDraftConsumer {

    private final EmailRepository emailRepository;
    private final DraftReplyRepository draftReplyRepository;

    /**
     * AI 초안 생성 결과 수신: email.draft 큐
     * 수신 메시지 구조:
     * {
     *   "request_id": "req_002",
     *   "emailId": 123,
     *   "draft_reply": "안녕하세요...",
     *   "reply_embedding": [0.21, 0.55, ...] // 384차원 float 배열
     * }
     */
    @RabbitListener(queues = RabbitMQConfig.DRAFT_QUEUE)
    @Transactional
    public void receiveDraftResult(Map<String, Object> message) {
        Long emailId = Long.valueOf(message.get("emailId").toString());
        String requestId = message.get("request_id").toString();

        log.info("AI 초안 생성 결과 수신: emailId={}, requestId={}", emailId, requestId);

        try {
            String draftReply = message.get("draft_reply") != null
                    ? message.get("draft_reply").toString() : "";

            // reply_embedding: List<Double> → float[] 변환 (VectorConverter가 VECTOR 바이너리로 저장)
            float[] replyEmbedding = null;
            if (message.get("reply_embedding") != null) {
                @SuppressWarnings("unchecked")
                java.util.List<Number> embeddingList = (java.util.List<Number>) message.get("reply_embedding");
                replyEmbedding = new float[embeddingList.size()];
                for (int i = 0; i < embeddingList.size(); i++) {
                    replyEmbedding[i] = embeddingList.get(i).floatValue();
                }
            }

            // 1. Email 조회 (subject, user 참조)
            Email email = emailRepository.findById(emailId)
                    .orElseThrow(() -> new IllegalStateException("이메일을 찾을 수 없습니다: " + emailId));

            String draftSubject = "Re: " + email.getSubject();

            // 2. DraftReplies 저장 (기존 초안 있으면 내용 갱신, 없으면 신규 생성)
            DraftReply draftReplyEntity = draftReplyRepository
                    .findByEmail_EmailId(emailId)
                    .orElse(null);

            if (draftReplyEntity != null) {
                // regenerate: 기존 초안 내용 갱신
                draftReplyEntity.updateContent(draftSubject, draftReply, replyEmbedding);
                draftReplyEntity.updateStatus(DraftStatus.PENDING_REVIEW);
            } else {
                // generate: 신규 초안 생성
                draftReplyEntity = DraftReply.builder()
                        .email(email)
                        .user(email.getUser())
                        .draftSubject(draftSubject)
                        .draftContent(draftReply)
                        .replyEmbedding(replyEmbedding)
                        .status(DraftStatus.PENDING_REVIEW)
                        .build();
            }
            draftReplyRepository.save(draftReplyEntity);

            // 3. Email status → PENDING_REVIEW 업데이트
            email.updateStatus(EmailStatus.PENDING_REVIEW);
            emailRepository.save(email);

            log.info("초안 저장 완료: emailId={}", emailId);

        } catch (Exception e) {
            log.error("초안 결과 처리 실패: emailId={}, error={}", emailId, e.getMessage(), e);
        }
    }
}
