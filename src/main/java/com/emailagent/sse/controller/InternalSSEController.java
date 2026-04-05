package com.emailagent.sse.controller;

import com.emailagent.sse.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * APP Pod → SSE Pod 내부 브로드캐스트 수신 엔드포인트 (SSE Pod 전용).
 *
 * POST /internal/sse/push
 * APP Pod의 SseBroadcastListener가 모든 SSE Pod에 이 엔드포인트를 호출한다.
 * SSE Pod는 자신의 emitter 목록에서 해당 emailId 알림을 전송하고,
 * emitter가 없으면 조용히 무시한다.
 */
@RestController
@RequestMapping("/internal/sse")
@RequiredArgsConstructor
@Profile("sse")
public class InternalSSEController {

    private final SseEmitterService sseEmitterService;

    @PostMapping("/push")
    public ResponseEntity<Void> push(@RequestBody Map<String, Object> body) {
        Object emailIdObj = body.get("email_id");
        if (emailIdObj instanceof Number num) {
            sseEmitterService.notifyIfPresent(num.longValue());
        }
        return ResponseEntity.ok().build();
    }
}
