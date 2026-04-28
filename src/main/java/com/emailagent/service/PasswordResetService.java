package com.emailagent.service;

import com.emailagent.domain.entity.User;
import com.emailagent.dto.request.auth.PasswordResetCodeRequest;
import com.emailagent.dto.request.auth.PasswordResetVerifyRequest;
import com.emailagent.dto.response.auth.BaseResponse;
import com.emailagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    /** 인증 코드 저장소: email → [code, expiryEpochSeconds] */
    private final ConcurrentHashMap<String, long[]> codeStore = new ConcurrentHashMap<>();

    private static final long CODE_TTL_SECONDS = 300; // 5분
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Step 1: 이름 + 이메일 검증 후 6자리 인증 코드 발송
     */
    @Transactional(readOnly = true)
    public BaseResponse sendCode(PasswordResetCodeRequest request) {
        // 이름·이메일 검증 — 일치 여부를 외부에 노출하지 않기 위해 동일 오류 메시지 사용
        User user = userRepository.findByEmail(request.getEmail())
                .filter(u -> u.isActive() && u.getName().equals(request.getName()))
                .orElseThrow(() -> new IllegalArgumentException("이름 또는 이메일이 올바르지 않습니다."));

        String code = generateCode();
        long expiry = Instant.now().getEpochSecond() + CODE_TTL_SECONDS;
        codeStore.put(request.getEmail(), new long[]{Long.parseLong(code), expiry});

        sendEmail(user.getEmail(), code);
        log.info("비밀번호 재설정 인증 코드 발송: email={}", user.getEmail());
        return new BaseResponse();
    }

    /**
     * Step 2: 인증 코드 검증 + 새 비밀번호 설정
     */
    @Transactional
    public BaseResponse verifyAndReset(PasswordResetVerifyRequest request) {
        long[] entry = codeStore.get(request.getEmail());

        if (entry == null) {
            throw new IllegalArgumentException("인증 코드가 존재하지 않습니다. 다시 요청해 주세요.");
        }

        long storedCode = entry[0];
        long expiry = entry[1];

        // 만료 검사
        if (Instant.now().getEpochSecond() > expiry) {
            codeStore.remove(request.getEmail());
            throw new IllegalArgumentException("인증 코드가 만료되었습니다. 다시 요청해 주세요.");
        }

        // 코드 일치 검사
        if (storedCode != Long.parseLong(request.getCode())) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않습니다.");
        }

        // 검증 성공 — 코드 즉시 제거(재사용 방지)
        codeStore.remove(request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        log.info("비밀번호 재설정 완료: email={}", user.getEmail());
        return new BaseResponse();
    }

    private String generateCode() {
        // 000000 ~ 999999 범위의 6자리 코드 (앞자리 0 포함하여 6자리로 포맷)
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private void sendEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("[이메일 에이전트] 비밀번호 재설정 인증 코드");
        message.setText(
                "안녕하세요.\n\n" +
                "비밀번호 재설정 인증 코드를 안내해 드립니다.\n\n" +
                "인증 코드: " + code + "\n\n" +
                "이 코드는 5분간 유효합니다.\n" +
                "본인이 요청하지 않은 경우 이 이메일을 무시해 주세요."
        );
        mailSender.send(message);
    }
}
