package com.emailagent.controller;

import com.emailagent.dto.request.auth.LoginRequest;
import com.emailagent.dto.request.auth.PasswordResetRequest;
import com.emailagent.dto.response.auth.AuthMeResponse;
import com.emailagent.dto.response.auth.BaseResponse;
import com.emailagent.dto.response.auth.TokenLoginResponse;
import com.emailagent.security.CurrentUser;
import com.emailagent.service.AuthService;
import com.emailagent.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

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
     * 없을 경우 X-Forwarded-For 첫 번째 값, 최후 수단으로 RemoteAddr 사용.
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

    /** POST /api/auth/password-reset — 비밀번호 찾기 (비로그인, 이름+이메일 검증 후 재설정) */
    @PostMapping("/password-reset")
    public ResponseEntity<BaseResponse> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        return ResponseEntity.ok(userService.resetPassword(request));
    }
}
