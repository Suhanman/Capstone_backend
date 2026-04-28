package com.emailagent.domain.entity;

import com.emailagent.domain.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "integrations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Integration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_id")
    private Long integrationId;

    // user_id UNIQUE → OneToOne 관계
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "provider", nullable = false, length = 50)
    @Builder.Default
    private String provider = "GOOGLE";

    @Column(name = "connected_email", nullable = false, length = 255, unique = true)
    private String connectedEmail;

    @Column(name = "external_account_id", length = 255)
    private String externalAccountId;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "granted_scopes", columnDefinition = "TEXT")
    private String grantedScopes;

    // 부분 연동(Granular Consent) — Gmail은 필수, Calendar는 선택
    @Column(name = "is_gmail_connected", nullable = false)
    @Builder.Default
    private boolean isGmailConnected = false;

    @Column(name = "is_calendar_connected", nullable = false)
    @Builder.Default
    private boolean isCalendarConnected = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false)
    @Builder.Default
    private SyncStatus syncStatus = SyncStatus.CONNECTED;

    /** Gmail history 동기화 기준점. Pub/Sub 처리 완료 시마다 갱신된다. */
    @Column(name = "last_history_id")
    private Long lastHistoryId;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── 비즈니스 메서드 ──────────────────────────────────────────────────────────

    /**
     * Google OAuth 토큰 교환 후 연동 정보 전체 갱신 (Granular Consent 적용)
     * isGmailConnected: Gmail scope 부여 여부 (필수이므로 항상 true로 호출됨)
     * isCalendarConnected: Calendar scope 부여 여부 (선택, 사용자 거부 시 false)
     */
    public void updateTokens(String accessToken, String refreshToken,
                             LocalDateTime tokenExpiresAt, String grantedScopes,
                             String connectedEmail, String externalAccountId,
                             boolean isGmailConnected, boolean isCalendarConnected) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiresAt = tokenExpiresAt;
        this.grantedScopes = grantedScopes;
        this.connectedEmail = connectedEmail;
        this.externalAccountId = externalAccountId;
        this.isGmailConnected = isGmailConnected;
        this.isCalendarConnected = isCalendarConnected;
        this.syncStatus = SyncStatus.CONNECTED;
        this.lastSyncedAt = LocalDateTime.now();
    }

    /**
     * 연동 상태만 변경 (일시정지, 재연동 등)
     */
    public void updateSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    /**
     * Calendar 단독 연동 해제 (토큰은 유지, Calendar scope만 비활성화)
     */
    public void disconnectCalendar() {
        this.isCalendarConnected = false;
    }

    /**
     * Gmail history 동기화 기준점 갱신.
     * Pub/Sub 처리 완료 시마다 호출하여 다음 조회의 startHistoryId로 사용한다.
     */
    public void updateLastHistoryId(Long historyId) {
        this.lastHistoryId = historyId;
    }
}
