package com.emailagent.controller;

import com.emailagent.dto.request.auth.IntegrationStatusUpdateRequest;
import com.emailagent.dto.response.auth.*;
import com.emailagent.security.CurrentUser;
import com.emailagent.service.GoogleOAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final GoogleOAuthService googleOAuthService;

    /**
     * GET /api/integrations/google/authorization-url
     * Google OAuth 인증 URL 발급 (state JWT 포함)
     */
    @GetMapping("/google/authorization-url")
    public ResponseEntity<AuthorizationUrlResponse> getAuthorizationUrl(@CurrentUser Long userId) {
        return ResponseEntity.ok(googleOAuthService.getAuthorizationUrl(userId));
    }

    /**
     * GET /api/integrations/google/callback
     * Google OAuth 콜백 처리 — state JWT로 사용자 식별 (Authorization 헤더 불필요)
     * Gmail scope 누락 시 403, Calendar scope 누락 시 is_calendar_connected=false로 정상 응답
     */
    @GetMapping("/google/callback")
    public ResponseEntity<CallbackResponse> handleCallback(
            @RequestParam String code,
            @RequestParam String state) throws IOException {
        return ResponseEntity.ok(googleOAuthService.handleCallback(code, state));
    }

    /**
     * GET /api/integrations/me
     * 현재 사용자의 Google 연동 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<IntegrationResponse> getMyIntegration(@CurrentUser Long userId) {
        return ResponseEntity.ok(googleOAuthService.getMyIntegration(userId));
    }

    /**
     * PATCH /api/integrations/me/status
     * 연동 상태 변경 (CONNECTED / DISCONNECTED / ERROR)
     */
    @PatchMapping("/me/status")
    public ResponseEntity<IntegrationStatusResponse> updateStatus(
            @CurrentUser Long userId,
            @Valid @RequestBody IntegrationStatusUpdateRequest request) {
        return ResponseEntity.ok(googleOAuthService.updateStatus(userId, request));
    }

    /**
     * DELETE /api/integrations/me
     * 전체 Google 연동 해제
     */
    @DeleteMapping("/me")
    public ResponseEntity<BaseResponse> deleteIntegration(@CurrentUser Long userId) {
        return ResponseEntity.ok(googleOAuthService.deleteIntegration(userId));
    }
}
