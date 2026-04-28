package com.emailagent.controller;

import com.emailagent.dto.request.auth.IntegrationStatusUpdateRequest;
import com.emailagent.dto.response.auth.*;
import com.emailagent.security.CurrentUser;
import com.emailagent.security.JwtTokenProvider;
import com.emailagent.service.GoogleOAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final GoogleOAuthService googleOAuthService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    // 기존 Gmail 연동 완료 후 이동할 프론트 경로 (이미 ?tab=email 포함)
    @Value("${app.frontend.email-integration-path:/app/settings?tab=email}")
    private String emailIntegrationPath;

    // 자동 로그인(기존 계정 Gmail 연동) 후 이동할 프론트 경로
    @Value("${app.google.signup-success-path:/app/dashboard}")
    private String signupSuccessPath;

    // 신규 유저 비밀번호 입력 페이지 경로
    @Value("${app.google.signup-register-path:/auth/google/register}")
    private String signupRegisterPath;

    @GetMapping("/google/authorization-url")
    public ResponseEntity<AuthorizationUrlResponse> getAuthorizationUrl(@CurrentUser Long userId) {
        return ResponseEntity.ok(googleOAuthService.getAuthorizationUrl(userId));
    }

    /**
     * Google OAuth 콜백 공통 처리.
     * state JWT의 mode로 INTEGRATION / SIGNUP 분기 후 각 프론트 경로로 redirect.
     *
     * INTEGRATION_DONE  → /app/settings?tab=email&google_oauth=success&...
     * AUTO_LOGIN        → /app/dashboard?google_oauth=auto_login&token=<JWT>
     * PENDING_REGISTRATION → /auth/google/register?temp_token=...&email=...&name=...
     */
    @GetMapping("/google/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestParam String code,
            @RequestParam String state) {
        try {
            OAuthCallbackResult result = googleOAuthService.handleCallback(code, state);

            String redirectUrl = switch (result.getType()) {
                case INTEGRATION_DONE -> frontendBaseUrl
                        + emailIntegrationPath
                        + "&google_oauth=success"
                        + "&gmail_connected=" + result.isGmailConnected()
                        + "&calendar_connected=" + result.isCalendarConnected();

                case AUTO_LOGIN -> frontendBaseUrl
                        + signupSuccessPath
                        + "?google_oauth=auto_login"
                        + "&token=" + URLEncoder.encode(result.getJwt(), StandardCharsets.UTF_8);

                case PENDING_REGISTRATION -> frontendBaseUrl
                        + signupRegisterPath
                        + "?temp_token=" + URLEncoder.encode(result.getTempToken(), StandardCharsets.UTF_8)
                        + "&email=" + URLEncoder.encode(result.getEmail(), StandardCharsets.UTF_8)
                        + "&name=" + URLEncoder.encode(result.getName() != null ? result.getName() : "", StandardCharsets.UTF_8);
            };

            return ResponseEntity.status(302)
                    .location(URI.create(redirectUrl))
                    .build();

        } catch (Exception e) {
            // 어느 모드에서 실패했는지 state JWT로 판별해 적절한 에러 페이지로 redirect
            boolean isSignupMode = false;
            try {
                isSignupMode = "SIGNUP".equals(jwtTokenProvider.getOAuthStateMode(state));
            } catch (Exception ignored) {}

            String message = URLEncoder.encode(
                    e.getMessage() != null ? e.getMessage() : "Google OAuth 처리에 실패했습니다.",
                    StandardCharsets.UTF_8);

            String errorRedirect = isSignupMode
                    ? frontendBaseUrl + signupRegisterPath + "?error=true&message=" + message
                    : frontendBaseUrl + emailIntegrationPath + "&google_oauth=error&message=" + message;

            return ResponseEntity.status(302)
                    .location(URI.create(errorRedirect))
                    .build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<IntegrationResponse> getMyIntegration(@CurrentUser Long userId) {
        return ResponseEntity.ok(googleOAuthService.getMyIntegration(userId));
    }

    @PatchMapping("/me/status")
    public ResponseEntity<IntegrationStatusResponse> updateStatus(
            @CurrentUser Long userId,
            @Valid @RequestBody IntegrationStatusUpdateRequest request) {
        return ResponseEntity.ok(googleOAuthService.updateStatus(userId, request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<BaseResponse> deleteIntegration(@CurrentUser Long userId) {
        return ResponseEntity.ok(googleOAuthService.deleteIntegration(userId));
    }
}
