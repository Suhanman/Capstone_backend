package com.emailagent.controller;

import com.emailagent.dto.request.auth.GoogleSignupRequest;
import com.emailagent.dto.request.auth.LoginRequest;
import com.emailagent.dto.request.auth.PasswordResetCodeRequest;
import com.emailagent.dto.request.auth.PasswordResetVerifyRequest;
import com.emailagent.dto.response.auth.AuthMeResponse;
import com.emailagent.dto.response.auth.AuthorizationUrlResponse;
import com.emailagent.dto.response.auth.BaseResponse;
import com.emailagent.dto.response.auth.TokenLoginResponse;
import com.emailagent.security.CurrentUser;
import com.emailagent.service.AuthService;
import com.emailagent.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import com.emailagent.service.GoogleOAuthService;
import com.emailagent.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final GoogleOAuthService googleOAuthService;

    /** POST /api/auth/tokens — 로그인, JWT Access Token 발급 */
    @PostMapping("/tokens")
    public ResponseEntity<TokenLoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        return ResponseEntity.ok(authService.login(request, clientIp));
    }

    /**
     * Nginx Ingress가 주입하는 X-Real-IP를 우선 사용.
     * 없을 경우 X-Forwarded-For 첫 번째 값, 최후 수단으로 RemoteAddr 사용한다.
     */
    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            ip = (forwarded != null && !forwarded.isBlank())
                    ? forwarded.split(",")[0].trim()
                    : request.getRemoteAddr();
        }
        return ip;
    }

    /** GET /api/auth/me — 현재 인증된 사용자 확인 */
    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> getAuthMe(@CurrentUser Long userId) {
        return ResponseEntity.ok(new AuthMeResponse(userId));
    }

    /** POST /api/auth/password-reset/code — Step 1: 이름+이메일 검증 후 인증 코드 발송 */
    @PostMapping("/password-reset/code")
    public ResponseEntity<BaseResponse> sendPasswordResetCode(@Valid @RequestBody PasswordResetCodeRequest request) {
        return ResponseEntity.ok(passwordResetService.sendCode(request));
    }

    /** POST /api/auth/password-reset/verify — Step 2: 인증 코드 검증 후 새 비밀번호 설정 */
    @PostMapping("/password-reset/verify")
    public ResponseEntity<BaseResponse> verifyAndResetPassword(@Valid @RequestBody PasswordResetVerifyRequest request) {
        return ResponseEntity.ok(passwordResetService.verifyAndReset(request));
    }

    /** GET /api/auth/google/signup-url — Google 회원가입용 OAuth URL 반환 (비로그인) */
    @GetMapping("/google/signup-url")
    public ResponseEntity<AuthorizationUrlResponse> getGoogleSignupUrl() {
        return ResponseEntity.ok(googleOAuthService.getSignupAuthorizationUrl());
    }

    /** POST /api/auth/google/signup — Google 회원가입 Step 2: temp_token + 비밀번호 → 계정 생성 */
    @PostMapping("/google/signup")
    public ResponseEntity<TokenLoginResponse> completeGoogleSignup(@Valid @RequestBody GoogleSignupRequest request) {
        return ResponseEntity.ok(googleOAuthService.completeGoogleSignup(request));
    }
}
