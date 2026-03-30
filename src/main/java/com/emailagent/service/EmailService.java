package com.emailagent.service;

import com.emailagent.domain.entity.Email;
import com.emailagent.domain.entity.Outbox;
import com.emailagent.domain.entity.User;
import com.emailagent.domain.enums.EmailStatus;
import com.emailagent.domain.enums.OutboxStatus;
import com.emailagent.dto.response.EmailDetailResponse;
import com.emailagent.dto.response.EmailListResponse;
import com.emailagent.dto.response.EmailPageResponse;
import com.emailagent.exception.EmailNotFoundException;
import com.emailagent.messaging.EmailMessagePublisher;
import com.emailagent.repository.EmailRepository;
import com.emailagent.repository.OutboxRepository;
import com.emailagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailRepository emailRepository;
    private final OutboxRepository outboxRepository;
    private final UserRepository userRepository;
    private final EmailMessagePublisher messagePublisher;

    /**
     * 이메일 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public EmailPageResponse getEmails(Long userId, Pageable pageable) {
        var page = emailRepository.findByUser_UserIdOrderByReceivedAtDesc(userId, pageable);
        return EmailPageResponse.builder()
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .number(page.getNumber())
                .size(page.getSize())
                .content(page.getContent().stream().map(EmailListResponse::from).toList())
                .build();
    }

    /**
     * 이메일 상세 조회
     */
    @Transactional(readOnly = true)
    public EmailDetailResponse getEmailDetail(Long emailId, Long userId) {
        Email email = emailRepository.findByEmailIdAndUserId(emailId, userId)
                .orElseThrow(() -> new EmailNotFoundException(emailId));
        return EmailDetailResponse.from(email);
    }

    /**
     * Gmail 웹훅으로 새 이메일 수신 처리
     * STEP 0: 수신 → Outbox 저장 → RabbitMQ 발행
     */
    @Transactional
    public void processIncomingEmail(Long userId, String externalMsgId,
                                      String senderName, String senderEmail,
                                      String subject, String bodyRaw,
                                      String bodyClean, LocalDateTime receivedAt) {

        // 중복 처리 방지
        if (emailRepository.existsByExternalMsgId(externalMsgId)) {
            log.warn("이미 처리된 이메일입니다: {}", externalMsgId);
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 1. Email 저장
        Email email = Email.builder()
                .user(user)
                .externalMsgId(externalMsgId)
                .senderName(senderName)
                .senderEmail(senderEmail)
                .subject(subject)
                .bodyRaw(bodyRaw)
                .bodyClean(bodyClean)
                .receivedAt(receivedAt)
                .build();
        email = emailRepository.save(email);

        // 2. Outbox 패턴: AI 처리 요청 큐잉
        Map<String, Object> payload = new HashMap<>();
        payload.put("email_id", email.getEmailId());
        payload.put("user_id", userId);
        payload.put("body_clean", bodyClean);
        payload.put("subject", subject);

        Outbox outbox = Outbox.builder()
                .email(email)
                .payload(payload)
                .build();
        outboxRepository.save(outbox);

        // 3. RabbitMQ로 AI 서버에 분석 요청 발행
        messagePublisher.publishEmailAnalysisRequest(outbox.getOutboxId(), payload);

        log.info("새 이메일 처리 시작: emailId={}, externalMsgId={}", email.getEmailId(), externalMsgId);
    }

    /**
     * 이메일 상태 업데이트
     */
    @Transactional
    public void updateEmailStatus(Long emailId, Long userId, EmailStatus status) {
        Email email = emailRepository.findByEmailIdAndUserId(emailId, userId)
                .orElseThrow(() -> new EmailNotFoundException(emailId));
        email.updateStatus(status);
    }
}
