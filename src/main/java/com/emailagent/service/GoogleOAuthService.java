package com.emailagent.service;

import com.emailagent.domain.entity.Integration;
import com.emailagent.domain.entity.User;
import com.emailagent.domain.enums.SyncStatus;
import com.emailagent.dto.request.auth.IntegrationStatusUpdateRequest;
import com.emailagent.dto.request.auth.DeleteIntegrationRequest;
import com.emailagent.dto.response.auth.*;
import com.emailagent.exception.InsufficientScopeException;
import com.emailagent.repository.IntegrationRepository;
import com.emailagent.repository.UserRepository;
import com.emailagent.security.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    // Google OAuth 요청 시 포함할 전체 스코프 (Gmail 필수 + Calendar 선택)
    private static final List<String> REQUEST_SCOPES = Arrays.asList(
            "email",
            "profile",
            "https://www.googleapis.com/auth/gmail.readonly",
            "https://www.googleapis.com/auth/calendar"
    );

    // 콜백 시 반드시 부여되어야 하는 필수 스코프 (없으면 연동 실패 처리)
    private static final String GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.readonly";

    // 선택 스코프 — 사용자가 거부해도 연동은 성공 (is_calendar_connected=false)
    private static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar";

    private final IntegrationRepository integrationRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.google.client-id}")
    private String clientId;

    @Value("${app.google.client-secret}")
    private String clientSecret;

    @Value("${app.google.redirect-uri}")
    private String redirectUri;

    // ── 1. Google OAuth 인증 URL 생성 ──────────────────────────────────────────

    /**
     * CSRF 방지용 state JWT(10분 만료)를 생성하고 Google OAuth 인증 URL을 반환한다.
     * access_type=offline → refresh_token 발급 보장
     * prompt=consent → 재동의 화면 강제 (refresh_token 재발급)
     */
    @Transactional(readOnly = true)
    public AuthorizationUrlResponse getAuthorizationUrl(Long userId) {
        String stateJwt = jwtTokenProvider.generateOAuthStateToken(userId);

        String url = new GoogleAuthorizationCodeRequestUrl(clientId, redirectUri, REQUEST_SCOPES)
                .setAccessType("offline")
                .set("prompt", "consent")
                .setState(stateJwt)
                .build();

        return new AuthorizationUrlResponse(url);
    }

    // ── 2. 콜백 처리 (코드 교환 → 스코프 검증 → DB 저장) ───────────────────────

    /**
     * Google 리다이렉트 콜백 처리 (Granular Consent 적용).
     * 1) state JWT 검증으로 userId 추출 (CSRF 방어)
     * 2) Authorization Code → Access/Refresh Token 교환
     * 3) 스코프 검증 — Gmail(필수) 누락 시 InsufficientScopeException, Calendar(선택) 누락은 허용
     * 4) id_token 파싱으로 이메일 및 구글 계정 ID 추출
     * 5) Integrations 테이블 upsert (is_gmail_connected / is_calendar_connected 저장)
     */
    @Transactional
    public CallbackResponse handleCallback(String code, String state) throws IOException {
        // 1. state JWT 검증 → userId 추출
        Long userId = jwtTokenProvider.getOAuthStateUserId(state);

        // 2. Authorization Code → Access/Refresh Token 교환
        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                clientId,
                clientSecret,
                code,
                redirectUri
        ).execute();

        // 3. 스코프 분석 — Gmail은 필수, Calendar는 선택
        String grantedScopesRaw = tokenResponse.getScope();
        List<String> grantedScopes = Arrays.asList(grantedScopesRaw.split(" "));

        boolean isGmailConnected = grantedScopes.contains(GMAIL_SCOPE);
        boolean isCalendarConnected = grantedScopes.contains(CALENDAR_SCOPE);

        // Gmail scope 미부여 시 연동 자체를 실패 처리
        if (!isGmailConnected) {
            throw new InsufficientScopeException("필수 메일 권한이 누락되었습니다. 다시 동의해 주세요.");
        }

        // 4. id_token 파싱으로 사용자 정보 추출
        GoogleIdToken idToken = tokenResponse.parseIdToken();
        GoogleIdToken.Payload payload = idToken.getPayload();
        String connectedEmail = payload.getEmail();
        String externalAccountId = payload.getSubject(); // Google 계정 고유 ID

        // 5. 토큰 만료 시각 계산
        LocalDateTime tokenExpiresAt = LocalDateTime.now()
                .plusSeconds(tokenResponse.getExpiresInSeconds());

        // 6. Integrations upsert (이미 연동됐으면 갱신, 없으면 신규 생성)
        final boolean gmailFlag = isGmailConnected;
        final boolean calendarFlag = isCalendarConnected;
        integrationRepository.findByUser_UserId(userId).ifPresentOrElse(
                existing -> existing.updateTokens(
                        tokenResponse.getAccessToken(),
                        tokenResponse.getRefreshToken(),
                        tokenExpiresAt,
                        grantedScopesRaw,
                        connectedEmail,
                        externalAccountId,
                        gmailFlag,
                        calendarFlag
                ),
                () -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                    Integration integration = Integration.builder()
                            .user(user)
                            .connectedEmail(connectedEmail)
                            .externalAccountId(externalAccountId)
                            .accessToken(tokenResponse.getAccessToken())
                            .refreshToken(tokenResponse.getRefreshToken())
                            .tokenExpiresAt(tokenExpiresAt)
                            .grantedScopes(grantedScopesRaw)
                            .isGmailConnected(gmailFlag)
                            .isCalendarConnected(calendarFlag)
                            .syncStatus(SyncStatus.CONNECTED)
                            .lastSyncedAt(LocalDateTime.now())
                            .build();
                    integrationRepository.save(integration);
                }
        );

        return new CallbackResponse(isGmailConnected, isCalendarConnected);
    }

    // ── 3. 연동 정보 조회 ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public IntegrationResponse getMyIntegration(Long userId) {
        Integration integration = findIntegration(userId);
        return new IntegrationResponse(integration);
    }

    // ── 4. 연동 상태 변경 ──────────────────────────────────────────────────────

    @Transactional
    public IntegrationStatusResponse updateStatus(Long userId, IntegrationStatusUpdateRequest request) {
        Integration integration = findIntegration(userId);
        integration.updateSyncStatus(request.getSyncStatus());
        return new IntegrationStatusResponse(integration);
    }

    // ── 5. 연동 해제 ───────────────────────────────────────────────────────────

    /**
     * 연동 해제.
     * - target_service=ALL: Integration 레코드 전체 삭제 (토큰 포함 모든 연동 정보 제거)
     * - target_service=CALENDAR: Calendar scope만 비활성화 (토큰 및 Gmail 연동 유지)
     */
    @Transactional
    public SuccessResponse deleteIntegration(Long userId, DeleteIntegrationRequest request) {
        Integration integration = findIntegration(userId);

        if ("CALENDAR".equals(request.getTargetService())) {
            // 캘린더 단독 해제 — 레코드는 유지, is_calendar_connected=false
            integration.disconnectCalendar();
        } else {
            // ALL — 레코드 전체 삭제
            integrationRepository.delete(integration);
        }

        return new SuccessResponse();
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────────────────────

    private Integration findIntegration(Long userId) {
        return integrationRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("연동 정보가 존재하지 않습니다."));
    }
}
