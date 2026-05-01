package com.emailagent.service;

import com.emailagent.domain.entity.User;
import com.emailagent.domain.enums.UserRole;
import com.emailagent.dto.request.auth.LoginRequest;
import com.emailagent.dto.request.auth.SignupRequest;
import com.emailagent.dto.response.auth.SignupResponse;
import com.emailagent.dto.response.auth.TokenLoginResponse;
import com.emailagent.exception.AdminIpDeniedException;
import com.emailagent.repository.UserRepository;
import com.emailagent.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.expiration}")
    private long jwtExpiration; // ms 단위

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .build();
        user = userRepository.save(user);

        return new SignupResponse(user);
    }

    /**
     * POST /api/auth/tokens — 로그인, JWT Access Token 발급
     * ADMIN 계정은 10.0.0.0/24 대역에서만 허용
     */
    @Transactional
    public TokenLoginResponse login(LoginRequest request, String clientIp) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!user.isActive()) {
            throw new IllegalStateException("비활성화된 계정입니다.");
        }

        // validateAdminIp(user, clientIp); // TODO: pfSense DNS Split Horizon 설정 완료 후 활성화

        user.updateLastLogin();

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getEmail());
        return new TokenLoginResponse(accessToken, jwtExpiration);
    }

    /**
     * ADMIN role인 경우 10.0.0.0/24 대역 여부를 검증한다.
     * IP 파싱 실패 시 fail-closed 정책으로 접근을 거부한다.
     */
    private void validateAdminIp(User user, String clientIp) {
        if (user.getRole() != UserRole.ADMIN) return;

        try {
            byte[] client  = InetAddress.getByName(clientIp).getAddress();
            byte[] network = InetAddress.getByName("10.0.0.0").getAddress();
            // /24 = 상위 3바이트(24비트) 일치 여부만 비교
            boolean allowed = client[0] == network[0]
                           && client[1] == network[1]
                           && client[2] == network[2];
            if (!allowed) {
                log.warn("[ADMIN IP DENIED] userId={} ip={}", user.getUserId(), clientIp);
                throw new AdminIpDeniedException(clientIp);
            }
        } catch (UnknownHostException e) {
            log.warn("[ADMIN IP PARSE FAIL] userId={} ip={}", user.getUserId(), clientIp);
            throw new AdminIpDeniedException(clientIp);
        }
    }
}
