package com.emailagent.sse.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE Emitter 관리 서비스 (SSE Pod 전용).
 *
 * [역할]
 * - 브라우저와의 SSE 연결 객체(SseEmitter)를 메모리에서 userId 기준으로 관리.
 * - createEmitter(): 신규 emitter 생성 및 등록.
 * - notifyIfPresent(): 해당 userId의 emitter가 있을 때만 이벤트 전송, 없으면 무시.
 *
 * [Pod 메모리 격리]
 * emitter는 이 Pod 메모리에만 존재. HAProxy Sticky Session으로 브라우저가
 * 항상 같은 SSE Pod에 연결되므로 다른 Pod의 notifyIfPresent()는 조용히 무시된다.
 */
@Slf4j
@Service
public class SseEmitterService {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30분

    /** userId → SseEmitter 매핑 (ConcurrentHashMap: 다중 요청 안전) */
    private final Map<Long, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    /**
     * 신규 SseEmitter 생성 및 등록.
     * 기존 연결이 있으면 교체(브라우저 재연결 시).
     *
     * @param userId 로그인 사용자 ID
     * @return 생성된 SseEmitter (Controller가 반환)
     */
    public SseEmitter createEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitterMap.put(userId, emitter);

        // 완료/타임아웃/에러 시 emitter 제거
        emitter.onCompletion(() -> emitterMap.remove(userId));
        emitter.onTimeout(() -> {
            emitterMap.remove(userId);
            emitter.complete();
        });
        emitter.onError(e -> emitterMap.remove(userId));

        log.debug("[SseEmitterService] emitter 등록 — userId={}", userId);
        return emitter;
    }

    /**
     * 해당 userId의 emitter가 있을 때만 이벤트 전송.
     * 없으면 조용히 무시 (다른 SSE Pod의 브로드캐스트 수신 시 정상 동작).
     *
     * @param emailId 완료된 이메일 ID (클라이언트에 전달)
     */
    public void notifyIfPresent(Long emailId) {
        // emailId 기준으로 어떤 userId에게 보낼지는 실제 서비스에서 조회 필요.
        // 현재는 단순 브로드캐스트: 모든 연결된 emitter에 알림.
        emitterMap.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("classify-complete")
                        .data(Map.of("email_id", emailId)));
                log.debug("[SseEmitterService] SSE 전송 — userId={}, emailId={}", userId, emailId);
            } catch (IOException e) {
                log.warn("[SseEmitterService] 전송 실패, emitter 제거 — userId={}", userId);
                emitterMap.remove(userId);
                emitter.completeWithError(e);
            }
        });
    }

    public void sendEventToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitterMap.get(userId);
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            log.debug("[SseEmitterService] SSE 전송 — userId={}, event={}", userId, eventName);
        } catch (IOException e) {
            log.warn("[SseEmitterService] 전송 실패, emitter 제거 — userId={}, event={}", userId, eventName);
            emitterMap.remove(userId);
            emitter.completeWithError(e);
        }
    }
}
