package com.emailagent.controller;

import com.emailagent.dto.request.auth.IntegrationStatusUpdateRequest;
import com.emailagent.dto.response.auth.*;
import com.emailagent.security.CurrentUser;
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

    @Value("${app.google.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @GetMapping("/google/authorization-url")
    public ResponseEntity<AuthorizationUrlResponse> getAuthorizationUrl(@CurrentUser Long userId) {
        return ResponseEntity.ok(googleOAuthService.getAuthorizationUrl(userId));
    }

    @GetMapping("/google/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestParam String code,
            @RequestParam String state) throws IOException {
        try {
            CallbackResponse response = googleOAuthService.handleCallback(code, state);

            String redirectUrl = frontendBaseUrl
                    + "/app/settings?tab=email"
                    + "&google_oauth=success"
                    + "&gmail_connected=" + response.isGmailConnected()
                    + "&calendar_connected=" + response.isCalendarConnected();

            return ResponseEntity.status(302)
                    .location(URI.create(redirectUrl))
                    .build();
        } catch (Exception e) {
            String message = URLEncoder.encode(
                    e.getMessage() == null ? "Google OAuth callback failed" : e.getMessage(),
                    StandardCharsets.UTF_8
            );

            String redirectUrl = frontendBaseUrl
                    + "/app/settings?tab=email"
                    + "&google_oauth=error"
                    + "&message=" + message;

            return ResponseEntity.status(302)
                    .location(URI.create(redirectUrl))
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