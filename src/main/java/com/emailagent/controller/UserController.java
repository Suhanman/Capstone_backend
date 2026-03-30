package com.emailagent.controller;

import com.emailagent.dto.request.auth.PasswordChangeRequest;
import com.emailagent.dto.request.auth.SignupRequest;
import com.emailagent.dto.request.auth.UserProfileUpdateRequest;
import com.emailagent.dto.response.auth.BaseResponse;
import com.emailagent.dto.response.auth.EmailAvailabilityResponse;
import com.emailagent.dto.response.auth.SignupResponse;
import com.emailagent.dto.response.auth.UserProfileResponse;
import com.emailagent.dto.response.auth.UserUpdateResponse;
import com.emailagent.security.CurrentUser;
import com.emailagent.service.AuthService;
import com.emailagent.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    /** POST /api/users — 회원가입 */
    @PostMapping
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@CurrentUser Long userId) {
        return ResponseEntity.ok(userService.getMyProfile(userId));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserUpdateResponse> updateMyProfile(
            @CurrentUser Long userId,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok(userService.updateMyProfile(userId, request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<BaseResponse> deleteMe(@CurrentUser Long userId) {
        return ResponseEntity.ok(userService.deleteMe(userId));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<BaseResponse> changePassword(
            @CurrentUser Long userId,
            @Valid @RequestBody PasswordChangeRequest request) {
        return ResponseEntity.ok(userService.changePassword(userId, request));
    }

    @GetMapping("/email-availability")
    public ResponseEntity<EmailAvailabilityResponse> checkEmailAvailability(
            @RequestParam String email) {
        return ResponseEntity.ok(userService.checkEmailAvailability(email));
    }
}
