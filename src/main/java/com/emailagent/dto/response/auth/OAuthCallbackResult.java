package com.emailagent.dto.response.auth;

import lombok.Getter;

/**
 * Google OAuth 콜백 결과를 컨트롤러에 전달하는 내부 전달 객체.
 * 세 가지 타입으로 분기:
 *   INTEGRATION_DONE — 기존 로그인 유저의 Gmail 연동 완료
 *   AUTO_LOGIN       — 이미 가입/연동된 Gmail → 자동 로그인
 *   PENDING_REGISTRATION — 신규 유저 → 프론트 회원가입 페이지로 이동
 */
@Getter
public class OAuthCallbackResult {

    public enum Type { INTEGRATION_DONE, AUTO_LOGIN, PENDING_REGISTRATION }

    private final Type type;

    // INTEGRATION_DONE
    private boolean gmailConnected;
    private boolean calendarConnected;

    // AUTO_LOGIN
    private String jwt;
    private long expiresIn;

    // PENDING_REGISTRATION
    private String tempToken;
    private String email;
    private String name;

    private OAuthCallbackResult(Type type) {
        this.type = type;
    }

    public static OAuthCallbackResult integrationDone(boolean gmailConnected, boolean calendarConnected) {
        OAuthCallbackResult r = new OAuthCallbackResult(Type.INTEGRATION_DONE);
        r.gmailConnected = gmailConnected;
        r.calendarConnected = calendarConnected;
        return r;
    }

    public static OAuthCallbackResult autoLogin(String jwt, long expiresInMs) {
        OAuthCallbackResult r = new OAuthCallbackResult(Type.AUTO_LOGIN);
        r.jwt = jwt;
        r.expiresIn = expiresInMs / 1000;
        return r;
    }

    public static OAuthCallbackResult pendingRegistration(String tempToken, String email, String name) {
        OAuthCallbackResult r = new OAuthCallbackResult(Type.PENDING_REGISTRATION);
        r.tempToken = tempToken;
        r.email = email;
        r.name = name;
        return r;
    }
}
