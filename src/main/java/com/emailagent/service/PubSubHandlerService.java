package com.emailagent.service;

import com.emailagent.domain.entity.Email;
import com.emailagent.domain.entity.Integration;
import com.emailagent.domain.entity.Outbox;
import com.emailagent.domain.entity.User;
import com.emailagent.dto.inbox.AttachmentMetaDto;
import com.emailagent.rabbitmq.event.SseEvent;
import com.emailagent.repository.EmailRepository;
import com.emailagent.repository.IntegrationRepository;
import com.emailagent.repository.OutboxRepository;
import com.emailagent.util.EmailParsingUtil;
import org.springframework.context.ApplicationEventPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
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
 * 처리 흐름 (6단계 파이프라인):
 * 1~2단계: messages.get(format=full) 조회 → headers(From/To/Subject/Date 등) 추출
 * 3~4단계: 본문 파트 재귀 탐색 → text/plain 우선, 없으면 text/html → Base64URL 디코딩
 * 5단계:   Jsoup HTML 제거 + 서명·인용문 제거 + 공백 정규화
 * 6단계:   AI 서버 전송 규격 JSON으로 payload 구성 → Email(attachments_meta 포함) 저장 → Outbox(READY) 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PubSubHandlerService {

    private final IntegrationRepository    integrationRepository;
    private final EmailRepository          emailRepository;
    private final OutboxRepository         outboxRepository;
    private final GoogleApiClientProvider  googleApiClientProvider;
    private final ObjectMapper             objectMapper;
    private final ApplicationEventPublisher eventPublisher;

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

            // 3. startHistoryId 결정:
            //    - lastHistoryId가 저장된 경우: 직전 처리 기준점 사용 (정상 경로)
            //    - 최초 수신 또는 기준점 없는 경우: Pub/Sub historyId - 1 사용 (fallback)
            //    Gmail API는 startHistoryId보다 큰 이력만 반환하므로,
            //    Pub/Sub에서 받은 historyId 자체가 아닌 이전 기준점을 넘겨야 한다.
            long startHistoryId;
            if (integration.getLastHistoryId() != null) {
                startHistoryId = integration.getLastHistoryId();
            } else {
                startHistoryId = historyId - 1;
            }

            ListHistoryResponse historyResponse = gmailClient.users().history()
                    .list("me")
                    .setStartHistoryId(BigInteger.valueOf(startHistoryId))
                    .setLabelId("INBOX")
                    .setHistoryTypes(List.of("messageAdded"))
                    .execute();

            if (historyResponse.getHistory() == null || historyResponse.getHistory().isEmpty()) {
                log.debug("[PubSub] 신규 메시지 없음 — startHistoryId={}, pubsubHistoryId={}, emailAddress={}",
                        startHistoryId, historyId, emailAddress);
                // 메시지 없어도 기준점은 갱신 (다음 알림을 위해)
                integration.updateLastHistoryId(historyId);
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

            // 5. 각 메시지를 6단계 파이프라인으로 파싱하여 Email(attachments_meta 포함) + Outbox 저장
            int savedCount = 0;
            for (String messageId : messageIds) {
                boolean saved = processMessage(gmailClient, user, integration.getConnectedEmail(), messageId);
                if (saved) savedCount++;
            }

            // 처리 완료 후 lastHistoryId 갱신 — 다음 Pub/Sub 알림의 startHistoryId 기준점
            integration.updateLastHistoryId(historyId);

            // 1건 이상 저장된 경우 SSE Hub에 알림 (트랜잭션 커밋 후 x.sse.fanout publish)
            // 복수 메시지가 동시에 저장되어도 신호는 1회면 충분 (클라이언트가 목록 재조회)
            if (savedCount > 0) {
                eventPublisher.publishEvent(new SseEvent(this, user.getUserId(), "pub/sub"));
            }

            log.debug("[PubSub] 처리 완료 — emailAddress={}, 신규 저장={}/{}건, lastHistoryId={}",
                    emailAddress, savedCount, messageIds.size(), historyId);

        } catch (Exception e) {
            // @Async 스레드 내 예외는 전역 핸들러가 잡지 못하므로 여기서 반드시 기록
            log.error("[PubSub] 비동기 처리 중 예외 발생 — emailAddress={}, historyId={}, error={}",
                    emailAddress, historyId, e.getMessage(), e);
        }
    }

    // ── 개별 메시지 처리 (6단계 파이프라인) ──────────────────────────────────────

    /**
     * Gmail 메시지 ID로 상세 조회 후 6단계 파이프라인을 수행하여
     * Email(attachments_meta JSON 포함), Outbox(READY)를 저장한다.
     *
     * externalMsgId 중복 시 건너뛰고 false 반환.
     */
    private boolean processMessage(Gmail gmailClient, User user, String connectedEmail, String messageId) throws Exception {
        // 중복 수신 방지 — Gmail historyId는 겹칠 수 있음
        if (emailRepository.existsByExternalMsgId(messageId)) {
            log.debug("[PubSub] 이미 처리된 메시지 — messageId={}", messageId);
            return false;
        }

        // 1~2단계: messages.get(format=full)로 전체 메시지 조회 후 헤더 추출
        Message message;
        try {
            message = gmailClient.users().messages()
                    .get("me", messageId)
                    .setFormat("full")
                    .execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                log.warn("[PubSub] Gmail 메시지를 찾을 수 없어 건너뜀 — messageId={}", messageId);
                return false;
            }
            throw e;
        }

        if (!isInboundInboxMessage(message, connectedEmail)) {
            log.debug("[PubSub] 수신함 처리 대상이 아닌 메시지 건너뜀 — messageId={}, labels={}",
                    messageId, message.getLabelIds());
            return false;
        }

        MessagePart payload = message.getPayload();
        Map<String, String> headers = EmailParsingUtil.parseHeaders(payload.getHeaders());

        String fromRaw  = headers.getOrDefault("From", "");
        String to       = headers.getOrDefault("To", "");
        String subject  = headers.getOrDefault("Subject", "(제목 없음)");
        String dateStr  = headers.getOrDefault("Date", "");

        String senderName  = EmailParsingUtil.extractSenderName(fromRaw);
        String senderEmail = EmailParsingUtil.extractSenderEmail(fromRaw);

        // ISO-8601 날짜 문자열 (Date 헤더 파싱 실패 시 internalDate fallback)
        String isoDate = EmailParsingUtil.formatEmailDate(dateStr, message.getInternalDate());

        // internalDate 기반 수신 시각 (DB 저장용 LocalDateTime)
        LocalDateTime receivedAt = message.getInternalDate() != null
                ? LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(message.getInternalDate()), ZoneId.of("Asia/Seoul"))
                : LocalDateTime.now();

        // 3~4단계: 본문 파트 재귀 탐색 + Base64URL 디코딩
        //          text/plain 우선, 없으면 text/html (attachment 파트 제외)
        String bodyRaw = EmailParsingUtil.extractBodyRaw(payload);
        boolean isHtml = EmailParsingUtil.isHtmlBody(payload);

        // 5단계: HTML 태그 제거(Jsoup), 서명·인용문·footer 제거, 공백 정규화
        String bodyClean = EmailParsingUtil.cleanBody(bodyRaw, isHtml);

        // 첨부파일 파트 수집 (attachments_meta JSON 구성에 사용)
        List<MessagePart> attachmentParts = EmailParsingUtil.collectAttachmentParts(payload);

        // ① attachments_meta JSON 구성 — 1-based 시퀀스 attachment_id 부여
        //    Gmail 실제 ID(String)는 gmail_attachment_id 필드에 별도 보관
        List<AttachmentMetaDto> attachmentMetaList = new ArrayList<>();
        for (int i = 0; i < attachmentParts.size(); i++) {
            MessagePart attPart = attachmentParts.get(i);
            attachmentMetaList.add(AttachmentMetaDto.builder()
                    .attachmentId(i + 1)
                    .gmailAttachmentId(attPart.getBody() != null
                            ? attPart.getBody().getAttachmentId() : null)
                    .fileName(attPart.getFilename())
                    .contentType(attPart.getMimeType())
                    .size(attPart.getBody() != null && attPart.getBody().getSize() != null
                            ? attPart.getBody().getSize().longValue() : null)
                    .build());
        }

        // ② Email 엔티티 저장 — has_attachments · attachments_meta 포함
        Email email = Email.builder()
                .user(user)
                .externalMsgId(messageId)
                .senderName(senderName)
                .senderEmail(senderEmail)
                .subject(subject)
                .bodyRaw(bodyRaw)
                .bodyClean(bodyClean)
                .receivedAt(receivedAt)
                .hasAttachments(!attachmentMetaList.isEmpty())
                .attachmentsMeta(attachmentMetaList.isEmpty() ? null : attachmentMetaList)
                .build();
        emailRepository.save(email);

        // ③ 6단계: AI 서버 전송용 payload JSON 구성 (규격 준수)
        // Outbox 패턴: payload만으로 MQ 발행이 완결되도록 email_id, sender_name 포함
        ObjectNode payloadNode = objectMapper.createObjectNode();
        payloadNode.put("email_id",      email.getEmailId());  // Outbox폴링만으로 식별 가능
        payloadNode.put("sender_name",   senderName);           // Email 엔티티 접근 불필요
        payloadNode.put("messageId",     messageId);
        payloadNode.put("threadId",      message.getThreadId());
        payloadNode.put("subject",       subject);
        payloadNode.put("from",          senderEmail);
        payloadNode.put("to",            to);
        payloadNode.put("date",          isoDate);
        payloadNode.put("body_raw",      bodyRaw);
        payloadNode.put("body_clean",    bodyClean);
        payloadNode.put("hasAttachments", !attachmentParts.isEmpty());

        ArrayNode attachmentsNode = objectMapper.createArrayNode();
        for (MessagePart attPart : attachmentParts) {
            ObjectNode attNode = objectMapper.createObjectNode();
            attNode.put("filename", attPart.getFilename());
            attNode.put("mimeType", attPart.getMimeType());
            attachmentsNode.add(attNode);
        }
        payloadNode.set("attachments", attachmentsNode);

        Map<String, Object> payloadMap = objectMapper.convertValue(payloadNode, new TypeReference<>() {});

        // ④ Outbox(READY) 저장 — MailScheduler가 폴링하여 RabbitMQ(q.ai.inbound)로 발행
        Outbox outbox = Outbox.builder()
                .email(email)
                .payload(payloadMap)
                .build();
        outboxRepository.save(outbox);

        log.debug("[PubSub] 메시지 저장 완료 — messageId={}, subject={}, senderEmail={}, attachments={}건",
                messageId, subject, senderEmail, attachmentParts.size());
        return true;
    }

    /**
     * Gmail history에는 발송함/임시보관함/휴지통 등 수신 처리 대상이 아닌 이벤트가 섞일 수 있다.
     * 저장 직전에 수신함에 남아 있는 외부 발신 메일만 통과시킨다.
     */
    private boolean isInboundInboxMessage(Message message, String connectedEmail) {
        List<String> labelIds = message.getLabelIds();
        if (labelIds == null || !labelIds.contains("INBOX")) {
            return false;
        }
        if (labelIds.contains("SENT")
                || labelIds.contains("DRAFT")
                || labelIds.contains("TRASH")
                || labelIds.contains("SPAM")) {
            return false;
        }

        MessagePart payload = message.getPayload();
        if (payload == null) {
            return false;
        }

        Map<String, String> headers = EmailParsingUtil.parseHeaders(payload.getHeaders());
        String senderEmail = EmailParsingUtil.extractSenderEmail(headers.getOrDefault("From", ""));
        return connectedEmail == null || !connectedEmail.equalsIgnoreCase(senderEmail);
    }
}
