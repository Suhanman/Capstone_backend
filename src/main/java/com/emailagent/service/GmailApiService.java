package com.emailagent.service;

import com.emailagent.domain.entity.Integration;
import com.emailagent.exception.EmailSendFailedException;
import com.emailagent.exception.GmailApiCallException;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.repository.IntegrationRepository;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartBody;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Properties;

/**
 * Gmail 공식 Java 클라이언트를 사용해 실제 메일 발송을 담당하는 서비스.
 *
 * 발송 흐름:
 * 1) userId로 Integration(토큰) 조회
 * 2) GoogleApiClientProvider로 Gmail 클라이언트 생성 (토큰 자동 갱신 포함)
 * 3) jakarta.mail MimeMessage 생성 → Base64URL 인코딩
 * 4) Gmail API users.messages.send() 호출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GmailApiService {

    private final IntegrationRepository integrationRepository;
    private final GoogleApiClientProvider googleApiClientProvider;

    /**
     * Gmail API로 메일을 발송한다.
     *
     * @param userId  발송 주체 사용자 ID (Integration 조회에 사용)
     * @param to      수신자 이메일 주소
     * @param subject 메일 제목
     * @param body    메일 본문 (plain text)
     * @throws EmailSendFailedException Gmail API 호출 실패 또는 MIME 생성 실패 시
     */
    @Transactional(readOnly = true)
    public void sendEmail(Long userId, String to, String subject, String body) {
        // 1. Integration 조회 — 발송에 사용할 Access/Refresh Token
        Integration integration = integrationRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Google 연동 정보를 찾을 수 없습니다. userId=" + userId));

        try {
            // 2. Gmail 클라이언트 생성 (Access Token 만료 시 Refresh Token으로 자동 갱신)
            Gmail gmailClient = googleApiClientProvider.buildGmailClient(integration);

            // 3. MimeMessage 생성 → Base64URL 인코딩
            // from 주소는 연동된 Google 계정 이메일 사용
            String rawMessage = buildRawMessage(integration.getConnectedEmail(), to, subject, body);

            // 4. Gmail API로 발송
            Message message = new Message();
            message.setRaw(rawMessage);
            gmailClient.users().messages().send("me", message).execute();

            log.info("[Gmail] 메일 발송 완료 — userId={}, to={}, subject={}", userId, to, subject);

        } catch (Exception e) {
            // 발송 실패 시 GlobalExceptionHandler가 처리할 수 있도록 커스텀 예외로 변환
            log.error("[Gmail] 메일 발송 실패 — userId={}, to={}, error={}", userId, to, e.getMessage(), e);
            throw new EmailSendFailedException(
                    "메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.", e);
        }
    }

    /**
     * Gmail API에서 특정 메시지의 첨부파일 데이터를 가져와 순수 byte[]로 반환한다.
     *
     * @param userId           발송 주체 사용자 ID
     * @param gmailMessageId   Gmail 메시지 ID (Email.externalMsgId)
     * @param gmailAttachmentId Gmail 첨부파일 ID (AttachmentMetaDto.gmailAttachmentId)
     * @return 첨부파일 원본 바이트 배열 (Base64URL 디코딩 완료)
     * @throws GmailApiCallException Gmail API 호출 실패 시
     */
    @Transactional(readOnly = true)
    public byte[] getAttachmentBytes(Long userId, String gmailMessageId, String gmailAttachmentId) {
        Integration integration = integrationRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Google 연동 정보를 찾을 수 없습니다. userId=" + userId));
        try {
            Gmail gmailClient = googleApiClientProvider.buildGmailClient(integration);

            // Gmail API: users.messages.attachments.get — Base64URL 인코딩 데이터 반환
            MessagePartBody partBody = gmailClient.users().messages().attachments()
                    .get("me", gmailMessageId, gmailAttachmentId)
                    .execute();

            // Base64URL → 순수 byte[] 변환 (Gmail API 규격)
            byte[] decoded = Base64.getUrlDecoder().decode(partBody.getData());

            log.info("[Gmail] 첨부파일 다운로드 완료 — userId={}, msgId={}, attachId={}, size={}bytes",
                    userId, gmailMessageId, gmailAttachmentId, decoded.length);
            return decoded;

        } catch (Exception e) {
            log.error("[Gmail] 첨부파일 다운로드 실패 — userId={}, msgId={}, attachId={}, error={}",
                    userId, gmailMessageId, gmailAttachmentId, e.getMessage(), e);
            throw new GmailApiCallException("첨부파일 다운로드에 실패했습니다. 잠시 후 다시 시도해 주세요.", e);
        }
    }

    /**
     * jakarta.mail MimeMessage를 생성하여 Gmail API가 요구하는 Base64URL 인코딩 문자열로 변환한다.
     * Session은 SMTP 연결 없이 메시지 구조 생성용으로만 사용한다.
     */
    private String buildRawMessage(String from, String to, String subject, String body) throws Exception {
        // SMTP 연결 없이 MimeMessage 구조 생성만을 위한 빈 Session
        Properties props = new Properties();
        Session session = Session.getInstance(props);

        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setFrom(new InternetAddress(from));
        mimeMessage.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        mimeMessage.setSubject(subject, "UTF-8");
        // plain text 본문 — AI 생성 답장이므로 HTML 불필요
        mimeMessage.setText(body, "UTF-8", "plain");

        // MimeMessage → 바이트 배열 → Base64URL 인코딩 (Gmail API 규격)
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        mimeMessage.writeTo(buffer);
        return Base64.getUrlEncoder().encodeToString(buffer.toByteArray());
    }
}
