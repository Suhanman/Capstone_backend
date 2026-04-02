package com.emailagent.service;

import com.emailagent.domain.entity.Email;
import com.emailagent.domain.entity.Integration;
import com.emailagent.domain.entity.Outbox;
import com.emailagent.domain.entity.User;
import com.emailagent.repository.EmailRepository;
import com.emailagent.repository.IntegrationRepository;
import com.emailagent.repository.OutboxRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Google Pub/Sub Push 알림의 비동기 처리 서비스.
 * WebhookController가 200 OK를 즉시 반환한 뒤 이 서비스가 별도 스레드에서 실행된다.
 *
 * 처리 흐름:
 * 1) connectedEmail로 Integration 조회 → 사용자 식별
 * 2) Gmail API로 historyId 이후 신규 메시지 목록 조회
 * 3) 각 메시지 본문 파싱 → ① Email 저장 → ② ObjectMapper로 payload 구성 → ③ Outbox(READY) 저장
 * 4) 이후 MailScheduler가 Outbox를 폴링하여 RabbitMQ로 발행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PubSubHandlerService {

    private final IntegrationRepository integrationRepository;
    private final EmailRepository emailRepository;
    private final OutboxRepository outboxRepository;
    private final GoogleApiClientProvider googleApiClientProvider;
    private final ObjectMapper objectMapper;

    /**
     * Pub/Sub Push 알림을 비동기로 처리한다.
     *
     * @Async 메서드는 메인 스레드와 분리되어 실행되므로
     * GlobalExceptionHandler가 예외를 잡지 못한다.
     * 따라서 최상위 try-catch로 방어하여 상세 에러 로그를 반드시 기록한다.
     */
    @Async
    @Transactional
    public void handleAsync(String emailAddress, Long historyId) {
        try {
            // 1. connectedEmail로 Integration 조회 — 어느 사용자에게 온 메일인지 식별
            Integration integration = integrationRepository.findByConnectedEmail(emailAddress)
                    .orElseThrow(() -> new IllegalStateException(
                            "연동 정보를 찾을 수 없습니다. emailAddress=" + emailAddress));

            if (!integration.isGmailConnected()) {
                log.warn("[PubSub] Gmail 연동 비활성 상태 — 처리 중단. emailAddress={}", emailAddress);
                return;
            }

            User user = integration.getUser();

            // 2. Gmail 클라이언트 생성 (토큰 만료 시 RefreshToken으로 자동 갱신)
            Gmail gmailClient = googleApiClientProvider.buildGmailClient(integration);

            // 3. historyId 이후 변경 이력 조회 — 신규 수신 메시지(messageAdded)만 필터링
            ListHistoryResponse historyResponse = gmailClient.users().history()
                    .list("me")
                    .setStartHistoryId(BigInteger.valueOf(historyId))
                    .setHistoryTypes(List.of("messageAdded"))
                    .execute();

            if (historyResponse.getHistory() == null || historyResponse.getHistory().isEmpty()) {
                log.info("[PubSub] 신규 메시지 없음 — historyId={}, emailAddress={}", historyId, emailAddress);
                return;
            }

            // 4. 신규 메시지 ID 수집 (LinkedHashSet으로 순서 유지 및 중복 제거)
            Set<String> messageIds = new LinkedHashSet<>();
            for (History history : historyResponse.getHistory()) {
                if (history.getMessagesAdded() != null) {
                    for (HistoryMessageAdded added : history.getMessagesAdded()) {
                        messageIds.add(added.getMessage().getId());
                    }
                }
            }

            // 5. 각 메시지를 파싱하여 Email + Outbox 저장
            int savedCount = 0;
            for (String messageId : messageIds) {
                boolean saved = processMessage(gmailClient, user, messageId);
                if (saved) savedCount++;
            }

            log.info("[PubSub] 처리 완료 — emailAddress={}, 신규 저장={}/{}건",
                    emailAddress, savedCount, messageIds.size());

        } catch (Exception e) {
            // @Async 스레드 내 예외는 전역 핸들러가 잡지 못하므로 여기서 반드시 기록
            log.error("[PubSub] 비동기 처리 중 예외 발생 — emailAddress={}, historyId={}, error={}",
                    emailAddress, historyId, e.getMessage(), e);
        }
    }

    // ── 개별 메시지 처리 ────────────────────────────────────────────────────────

    /**
     * Gmail 메시지 ID로 본문을 조회하여 Email 엔티티와 Outbox(READY)를 저장한다.
     *
     * 저장 순서:
     * ① Email 엔티티 저장 (email_id 획득)
     * ② ObjectMapper로 AI 전송용 payload JSON 구성
     * ③ Outbox(READY) 저장
     *
     * externalMsgId 중복 시 건너뛰고 false 반환.
     */
    private boolean processMessage(Gmail gmailClient, User user, String messageId) throws Exception {
        // 중복 수신 방지 — Gmail historyId는 겹칠 수 있음
        if (emailRepository.existsByExternalMsgId(messageId)) {
            log.debug("[PubSub] 이미 처리된 메시지 — messageId={}", messageId);
            return false;
        }

        // Gmail API로 전체 메시지 조회 (헤더 + 본문 포함)
        Message message = gmailClient.users().messages()
                .get("me", messageId)
                .setFormat("full")
                .execute();

        // 헤더에서 발신자/제목 추출
        Map<String, String> headers = parseHeaders(message.getPayload().getHeaders());
        String senderRaw = headers.getOrDefault("From", "");
        String subject   = headers.getOrDefault("Subject", "(제목 없음)");

        String senderName  = extractSenderName(senderRaw);
        String senderEmail = extractSenderEmail(senderRaw);

        // internalDate(ms) 기준으로 수신 시각 변환
        LocalDateTime receivedAt = message.getInternalDate() != null
                ? LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(message.getInternalDate()),
                        ZoneId.of("Asia/Seoul"))
                : LocalDateTime.now();

        // 본문 추출 — plain text 우선, 없으면 HTML 태그 제거
        String bodyRaw   = extractBody(message.getPayload(), "text/html");
        String bodyClean = extractBody(message.getPayload(), "text/plain");
        if (bodyClean.isBlank()) {
            bodyClean = stripHtml(bodyRaw);
        }

        // ① Email 엔티티 저장 — email_id 획득
        Email email = Email.builder()
                .user(user)
                .externalMsgId(messageId)
                .senderName(senderName)
                .senderEmail(senderEmail)
                .subject(subject)
                .bodyRaw(bodyRaw)
                .bodyClean(bodyClean)
                .receivedAt(receivedAt)
                .build();
        emailRepository.save(email);

        // ② ObjectMapper로 AI 서버 전송용 payload 구성
        // email_id, user_id, 제목, 발신자, 정제 본문을 JSON 구조로 조합
        ObjectNode payloadNode = objectMapper.createObjectNode();
        payloadNode.put("email_id",    email.getEmailId());
        payloadNode.put("user_id",     user.getUserId());
        payloadNode.put("subject",     subject);
        payloadNode.put("sender_email", senderEmail);
        payloadNode.put("body_clean",  bodyClean);

        Map<String, Object> payload = objectMapper.convertValue(payloadNode, new TypeReference<>() {});

        // ③ Outbox(READY) 저장 — MailScheduler가 폴링하여 RabbitMQ(q.ai.inbound)로 발행
        Outbox outbox = Outbox.builder()
                .email(email)
                .payload(payload)
                .build();
        outboxRepository.save(outbox);

        log.info("[PubSub] 메시지 저장 완료 — messageId={}, subject={}, senderEmail={}",
                messageId, subject, senderEmail);
        return true;
    }

    // ── 헤더 파싱 헬퍼 ──────────────────────────────────────────────────────────

    private Map<String, String> parseHeaders(List<MessagePartHeader> headers) {
        Map<String, String> map = new HashMap<>();
        if (headers == null) return map;
        for (MessagePartHeader h : headers) {
            map.put(h.getName(), h.getValue());
        }
        return map;
    }

    /** "홍길동 <user@gmail.com>" 형식에서 이름만 추출 */
    private String extractSenderName(String from) {
        int lt = from.indexOf('<');
        if (lt > 0) {
            return from.substring(0, lt).trim().replace("\"", "");
        }
        return "";
    }

    /** "홍길동 <user@gmail.com>" 또는 "user@gmail.com" 에서 이메일만 추출 */
    private String extractSenderEmail(String from) {
        int lt = from.indexOf('<');
        int gt = from.indexOf('>');
        if (lt >= 0 && gt > lt) {
            return from.substring(lt + 1, gt).trim();
        }
        return from.trim();
    }

    // ── 본문 추출 헬퍼 ──────────────────────────────────────────────────────────

    /**
     * Gmail 메시지 파트를 재귀 탐색하여 지정 mimeType의 본문을 추출한다.
     * Gmail은 multipart 구조이므로 parts 배열을 재귀적으로 탐색해야 한다.
     * 데이터는 Base64URL 인코딩 상태로 들어오므로 디코딩하여 반환한다.
     */
    private String extractBody(MessagePart part, String targetMime) {
        if (part == null) return "";

        if (targetMime.equals(part.getMimeType())
                && part.getBody() != null
                && part.getBody().getData() != null) {
            return new String(Base64.getUrlDecoder().decode(part.getBody().getData()));
        }

        if (part.getParts() != null) {
            for (MessagePart subPart : part.getParts()) {
                String result = extractBody(subPart, targetMime);
                if (!result.isBlank()) return result;
            }
        }
        return "";
    }

    /** AI 전달용 순수 텍스트 생성 — HTML 태그와 연속 공백 제거 */
    private String stripHtml(String html) {
        if (html == null || html.isBlank()) return "";
        return html.replaceAll("<[^>]*>", " ")
                   .replaceAll("\\s{2,}", " ")
                   .trim();
    }
}
