package com.emailagent.controller;

import com.emailagent.dto.request.webhook.PubSubMessageData;
import com.emailagent.dto.request.webhook.PubSubPushRequest;
import com.emailagent.dto.response.auth.BaseResponse;
import com.emailagent.service.PubSubHandlerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

/**
 * Google Pub/Sub Push 알림 수신 Webhook 컨트롤러.
 *
 * 핵심 원칙:
 * - 구글은 200 계열 응답을 받지 못하면 메시지를 재전송하므로,
 *   성공/실패/보안 오류 무관하게 항상 HTTP 200 OK 반환.
 * - 토큰 검증과 Base64 디코딩만 동기 처리 후 즉시 200 반환.
 * - 실제 Gmail API 호출 및 DB 저장은 PubSubHandlerService(@Async)에 위임.
 */
@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final PubSubHandlerService pubSubHandlerService;
    private final ObjectMapper objectMapper;

    @Value("${app.webhook.pubsub-verify-token:}")
    private String pubsubVerifyToken;

    /**
     * POST /api/webhook/pubsub
     *
     * Google Pub/Sub이 새 메일 알림을 Push할 때 호출된다.
     * Pub/Sub 구독 설정 시 push endpoint URL에 ?token=SECRET 을 붙여 보안 검증에 사용한다.
     */
    @PostMapping("/pubsub")
    public ResponseEntity<BaseResponse> receivePubSubPush(
            @RequestParam(value = "token", required = false) String token,
            @RequestBody PubSubPushRequest request) {

        // 1. 토큰 검증 — 실패해도 200 반환 (재전송 폭탄 방지), 경고 로그만 기록
        if (!isValidToken(token)) {
            log.warn("[Webhook] Pub/Sub 토큰 검증 실패 — 요청 무시. receivedToken={}", token);
            return ResponseEntity.ok(new BaseResponse());
        }

        // 2. 메시지 데이터 유효성 확인
        if (request.getMessage() == null || request.getMessage().getData() == null) {
            log.warn("[Webhook] Pub/Sub 메시지 데이터 없음 — 요청 무시. subscription={}",
                    request.getSubscription());
            return ResponseEntity.ok(new BaseResponse());
        }

        try {
            // 3. message.data Base64 디코딩 → emailAddress, historyId 추출
            byte[] decoded = Base64.getDecoder().decode(request.getMessage().getData());
            PubSubMessageData messageData = objectMapper.readValue(decoded, PubSubMessageData.class);

            log.info("[Webhook] Pub/Sub Push 수신 — emailAddress={}, historyId={}, messageId={}",
                    messageData.getEmailAddress(),
                    messageData.getHistoryId(),
                    request.getMessage().getMessageId());

            // 4. 비동기 처리 위임 후 즉시 200 반환 (Gmail API 호출은 별도 스레드에서 실행)
            pubSubHandlerService.handleAsync(messageData.getEmailAddress(), messageData.getHistoryId());

        } catch (Exception e) {
            // 파싱 실패도 재전송 방지를 위해 200 반환, 에러 로그만 기록
            log.error("[Webhook] Pub/Sub 메시지 파싱 실패 — messageId={}, error={}",
                    request.getMessage().getMessageId(), e.getMessage(), e);
        }

        return ResponseEntity.ok(new BaseResponse());
    }

    /**
     * 쿼리 파라미터 토큰 검증.
     * PUBSUB_VERIFY_TOKEN 환경변수가 설정되지 않은 경우(개발 환경)에는 검증을 생략한다.
     */
    private boolean isValidToken(String token) {
        if (pubsubVerifyToken == null || pubsubVerifyToken.isBlank()) {
            log.debug("[Webhook] PUBSUB_VERIFY_TOKEN 미설정 — 토큰 검증 생략 (개발 환경)");
            return true;
        }
        return pubsubVerifyToken.equals(token);
    }
}
