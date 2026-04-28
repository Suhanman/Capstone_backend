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

    // Google OAuth 팝업 종료를 담당하는 프론트 콜백 경로
    @Value("${app.frontend.oauth-callback-path:/oauth/google/callback}")
    private String oauthCallbackPath;

    @GetMapping("/google/authorization-url")
    public ResponseEntity<AuthorizationUrlResponse> getAuthorizationUrl(@CurrentUser Long userId) {
        return ResponseEntity.ok(googleOAuthService.getAuthorizationUrl(userId));
    }

    /**
     * Google OAuth 콜백 공통 처리.
     * state JWT의 mode로 INTEGRATION / SIGNUP 분기 후 프론트 공통 콜백 경로로 redirect.
     */
    @GetMapping("/google/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestParam String code,
            @RequestParam String state) {
        try {
            OAuthCallbackResult result = googleOAuthService.handleCallback(code, state);

            String redirectUrl = switch (result.getType()) {
                case INTEGRATION_DONE -> buildOAuthCallbackUrl(
                        "google_oauth=success"
                                + "&gmail_connected=" + result.isGmailConnected()
                                + "&calendar_connected=" + result.isCalendarConnected()
                );

                case AUTO_LOGIN -> buildOAuthCallbackUrl(
                        "google_oauth=auto_login"
                                + "&token=" + encode(result.getJwt())
                );

                case PENDING_REGISTRATION -> buildOAuthCallbackUrl(
                        "google_oauth=pending_registration"
                                + "&temp_token=" + encode(result.getTempToken())
                                + "&email=" + encode(result.getEmail())
                                + "&name=" + encode(result.getName())
                );
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

            String errorRedirect = buildOAuthCallbackUrl(
                    "google_oauth=error"
                            + "&mode=" + (isSignupMode ? "signup" : "integration")
                            + "&message=" + message
            );

            return ResponseEntity.status(302)
                    .location(URI.create(errorRedirect))
                    .build();
        }
    }

    private String buildOAuthCallbackUrl(String queryString) {
        String separator = oauthCallbackPath.contains("?") ? "&" : "?";
        return frontendBaseUrl + oauthCallbackPath + separator + queryString;
    }

    private String encode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
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
