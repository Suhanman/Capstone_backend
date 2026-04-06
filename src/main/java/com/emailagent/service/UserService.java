package com.emailagent.service;

import com.emailagent.domain.entity.User;
import com.emailagent.dto.request.auth.PasswordChangeRequest;
import com.emailagent.dto.request.auth.PasswordResetRequest;
import com.emailagent.dto.request.auth.UserProfileUpdateRequest;
import com.emailagent.dto.response.auth.BaseResponse;
import com.emailagent.dto.response.auth.EmailAvailabilityResponse;
import com.emailagent.dto.response.auth.UserProfileResponse;
import com.emailagent.dto.response.auth.UserUpdateResponse;
import com.emailagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = findActiveUser(userId);
        return new UserProfileResponse(user);
    }

    @Transactional
    public UserUpdateResponse updateMyProfile(Long userId, UserProfileUpdateRequest request) {
        User user = findActiveUser(userId);
        user.updateName(request.getName());
        return new UserUpdateResponse(user);
    }

    @Transactional
    public BaseResponse deleteMe(Long userId) {
        User user = findActiveUser(userId);
        user.deactivate();
        return new BaseResponse();
    }

    @Transactional(readOnly = true)
    public EmailAvailabilityResponse checkEmailAvailability(String email) {
        boolean available = !userRepository.existsByEmail(email);
        return new EmailAvailabilityResponse(available);
    }

    @Transactional
    public BaseResponse changePassword(Long userId, PasswordChangeRequest request) {
        User user = findActiveUser(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadCredentialsException("현재 비밀번호가 올바르지 않습니다.");
        }
        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        return new BaseResponse();
    }

    /**
     * POST /api/auth/password-reset — 비로그인 상태에서 비밀번호 찾기
     * 이름 + 이메일로 본인 확인 후 새 비밀번호로 변경
     */
    @Transactional
    public BaseResponse resetPassword(PasswordResetRequest request) {
        // 이메일로 사용자 조회 — 없을 경우 보안상 동일한 메시지 반환
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이름 또는 이메일이 올바르지 않습니다."));

        if (!user.isActive()) {
            throw new IllegalStateException("비활성화된 계정입니다.");
        }

        // 이름 일치 여부 검증
        if (!user.getName().equals(request.getName())) {
            throw new IllegalArgumentException("이름 또는 이메일이 올바르지 않습니다.");
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        return new BaseResponse();
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (!user.isActive()) {
            throw new IllegalStateException("비활성화된 계정입니다.");
        }
        return user;
    }
}
