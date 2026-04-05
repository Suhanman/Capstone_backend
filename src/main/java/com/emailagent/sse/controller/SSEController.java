package com.emailagent.sse.controller;

import com.emailagent.security.CurrentUser;
import com.emailagent.sse.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 프론트엔드 SSE 연결 수립 엔드포인트 (SSE Pod 전용).
 *
 * GET /api/mail/stream
 * HAProxy Sticky Session에 의해 브라우저는 항상 같은 SSE Pod에 연결된다.
 */
@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
@Profile("sse")
public class SSEController {

    private final SseEmitterService sseEmitterService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@CurrentUser Long userId) {
        return sseEmitterService.createEmitter(userId);
    }
}
