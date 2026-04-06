package com.emailagent.controller;

import com.emailagent.dto.request.auth.LoginRequest;
import com.emailagent.dto.request.auth.PasswordResetRequest;
import com.emailagent.dto.response.auth.AuthMeResponse;
import com.emailagent.dto.response.auth.BaseResponse;
import com.emailagent.dto.response.auth.TokenLoginResponse;
import com.emailagent.security.CurrentUser;
import com.emailagent.service.AuthService;
import com.emailagent.service.UserService;
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
    public ResponseEntity<TokenLoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
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
