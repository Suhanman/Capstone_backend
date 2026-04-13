# Gmail Pub/Sub historyId 버그 수정

## 문제 요약

`PubSubHandlerService.java`에서 Gmail `history().list()`의 `startHistoryId`로
Pub/Sub 알림에서 수신한 `historyId`를 그대로 사용하는 버그.

Gmail API는 `startHistoryId=X`일 때 **X보다 큰** 이력만 반환하므로,
Pub/Sub이 보낸 현재 이벤트의 `historyId`를 그대로 넣으면 항상 빈 결과를 반환한다.

**올바른 방식:** `Integration` 엔티티에 `lastHistoryId`를 저장해두고,
그 값을 `startHistoryId`로 사용 → 처리 완료 후 새 `historyId`로 갱신.

---

## 수정 파일 목록

1. `src/main/java/com/emailagent/domain/entity/Integration.java`
2. `src/main/java/com/emailagent/service/PubSubHandlerService.java`

---

## 1. Integration.java 수정

### 추가할 필드 (`syncStatus` 아래)

```java
@Column(name = "last_history_id")
private Long lastHistoryId;
```

### 추가할 비즈니스 메서드

```java
/**
 * Gmail history 동기화 기준점 갱신.
 * Pub/Sub 처리 완료 시마다 호출하여 다음 조회의 startHistoryId로 사용한다.
 */
public void updateLastHistoryId(Long historyId) {
    this.lastHistoryId = historyId;
}
```

### 완성된 Integration.java 전체 (수정 후)

```java
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "provider", nullable = false, length = 50)
    @Builder.Default
    private String provider = "GOOGLE";

    @Column(name = "connected_email", nullable = false, length = 255)
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

    public void updateSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

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
```

---

## 2. PubSubHandlerService.java 수정

### 수정 대상: `handleAsync()` 메서드 내 history 조회 구간

#### 수정 전 (버그)

```java
// 3. historyId 이후 변경 이력 조회 — 신규 수신 메시지(messageAdded)만 필터링
ListHistoryResponse historyResponse = gmailClient.users().history()
        .list("me")
        .setStartHistoryId(BigInteger.valueOf(historyId))  // ← Pub/Sub 수신값 그대로 사용 (버그)
        .setHistoryTypes(List.of("messageAdded"))
        .execute();

if (historyResponse.getHistory() == null || historyResponse.getHistory().isEmpty()) {
    log.debug("[PubSub] 신규 메시지 없음 — historyId={}, emailAddress={}", historyId, emailAddress);
    return;
}
```

#### 수정 후 (올바른 방식)

```java
// 3. startHistoryId 결정:
//    - lastHistoryId가 저장된 경우: 직전 처리 기준점 사용 (정상 경로)
//    - 최초 수신 또는 기준점 없는 경우: Pub/Sub historyId - 1 사용 (fallback)
//    Gmail API는 startHistoryId보다 큰 이력만 반환하므로,
//    Pub/Sub에서 받은 historyId 자체가 아닌 이전 기준점을 넘겨야 한다.
long startHistoryId;
if (integration.getLastHistoryId() != null) {
    startHistoryId = integration.getLastHistoryId();
} else {
    // 최초 연동 시 기준점 없음 → Pub/Sub historyId - 1로 현재 이벤트 포함
    startHistoryId = historyId - 1;
}

ListHistoryResponse historyResponse = gmailClient.users().history()
        .list("me")
        .setStartHistoryId(BigInteger.valueOf(startHistoryId))
        .setHistoryTypes(List.of("messageAdded"))
        .execute();

if (historyResponse.getHistory() == null || historyResponse.getHistory().isEmpty()) {
    log.debug("[PubSub] 신규 메시지 없음 — startHistoryId={}, pubsubHistoryId={}, emailAddress={}",
            startHistoryId, historyId, emailAddress);
    // 메시지 없어도 기준점은 갱신 (다음 알림을 위해)
    integration.updateLastHistoryId(historyId);
    return;
}
```

### 수정 대상: 처리 완료 후 기준점 갱신 (savedCount 로그 직전에 추가)

#### 수정 전

```java
log.debug("[PubSub] 처리 완료 — emailAddress={}, 신규 저장={}/{}건",
        emailAddress, savedCount, messageIds.size());
```

#### 수정 후

```java
// 처리 완료 후 lastHistoryId 갱신 — 다음 Pub/Sub 알림의 startHistoryId 기준점
integration.updateLastHistoryId(historyId);

log.debug("[PubSub] 처리 완료 — emailAddress={}, 신규 저장={}/{}건, lastHistoryId={}",
        emailAddress, savedCount, messageIds.size(), historyId);
```

### 완성된 handleAsync() 메서드 전체 (수정 후)

```java
@Async
@Transactional
public void handleAsync(String emailAddress, Long historyId) {
    try {
        // 1. connectedEmail로 Integration 조회 — 어느 사용자에게 온 메일인지 식별
        Integration integration = integrationRepository.findByConnectedEmail(emailAddress)
                .orElseThrow(() -> new IllegalStateException(
                        "연동 정보를 찾을 수 없습니다. emailAddress=" + emailAddress));

        if (!integration.isGmailConnected()) {
            log.warn("[PubSub] Gmail 연동 비활성 상태 — 처리 중단. emailAddress={}", emailAddress);
            return;
        }

        User user = integration.getUser();

        // 2. Gmail 클라이언트 생성 (토큰 만료 시 RefreshToken으로 자동 갱신)
        Gmail gmailClient = googleApiClientProvider.buildGmailClient(integration);

        // 3. startHistoryId 결정:
        //    - lastHistoryId가 저장된 경우: 직전 처리 기준점 사용 (정상 경로)
        //    - 최초 수신 또는 기준점 없는 경우: Pub/Sub historyId - 1 사용 (fallback)
        //    Gmail API는 startHistoryId보다 큰 이력만 반환하므로,
        //    Pub/Sub에서 받은 historyId 자체가 아닌 이전 기준점을 넘겨야 한다.
        long startHistoryId;
        if (integration.getLastHistoryId() != null) {
            startHistoryId = integration.getLastHistoryId();
        } else {
            startHistoryId = historyId - 1;
        }

        ListHistoryResponse historyResponse = gmailClient.users().history()
                .list("me")
                .setStartHistoryId(BigInteger.valueOf(startHistoryId))
                .setHistoryTypes(List.of("messageAdded"))
                .execute();

        if (historyResponse.getHistory() == null || historyResponse.getHistory().isEmpty()) {
            log.debug("[PubSub] 신규 메시지 없음 — startHistoryId={}, pubsubHistoryId={}, emailAddress={}",
                    startHistoryId, historyId, emailAddress);
            integration.updateLastHistoryId(historyId);
            return;
        }

        // 4. 신규 메시지 ID 수집 (LinkedHashSet으로 순서 유지 및 중복 제거)
        Set<String> messageIds = new LinkedHashSet<>();
        for (History history : historyResponse.getHistory()) {
            if (history.getMessagesAdded() != null) {
                for (HistoryMessageAdded added : history.getMessagesAdded()) {
                    messageIds.add(added.getMessage().getId());
                }
            }
        }

        // 5. 각 메시지를 6단계 파이프라인으로 파싱하여 Email + Outbox 저장
        int savedCount = 0;
        for (String messageId : messageIds) {
            boolean saved = processMessage(gmailClient, user, messageId);
            if (saved) savedCount++;
        }

        // 처리 완료 후 lastHistoryId 갱신 — 다음 Pub/Sub 알림의 startHistoryId 기준점
        integration.updateLastHistoryId(historyId);

        log.debug("[PubSub] 처리 완료 — emailAddress={}, 신규 저장={}/{}건, lastHistoryId={}",
                emailAddress, savedCount, messageIds.size(), historyId);

    } catch (Exception e) {
        log.error("[PubSub] 비동기 처리 중 예외 발생 — emailAddress={}, historyId={}, error={}",
                emailAddress, historyId, e.getMessage(), e);
    }
}
```

---

## Rollback 방법

버그 수정을 되돌려야 할 경우:

### Integration.java 롤백
- `lastHistoryId` 필드 제거
- `updateLastHistoryId()` 메서드 제거

### PubSubHandlerService.java 롤백
`handleAsync()` 내 history 조회 부분을 아래로 교체:

```java
ListHistoryResponse historyResponse = gmailClient.users().history()
        .list("me")
        .setStartHistoryId(BigInteger.valueOf(historyId))
        .setHistoryTypes(List.of("messageAdded"))
        .execute();

if (historyResponse.getHistory() == null || historyResponse.getHistory().isEmpty()) {
    log.debug("[PubSub] 신규 메시지 없음 — historyId={}, emailAddress={}", historyId, emailAddress);
    return;
}
```

처리 완료 후 `integration.updateLastHistoryId(historyId);` 줄 제거.

---

## 주의사항

- `Integration` 엔티티는 `@Transactional` 범위 안에서 수정되므로 별도 `save()` 호출 불필요 (Dirty Checking).
- `lastHistoryId`가 너무 오래된 경우 Gmail API가 `400 Invalid History Id` 에러를 반환할 수 있음.
  이 경우 `catch` 블록에서 `integration.updateLastHistoryId(null)` 처리를 추가하면 자동 복구 가능.
- DB 컬럼(`last_history_id BIGINT NULL`)은 JPA `ddl-auto`가 `update`/`create`면 자동 추가되나,
  운영 환경에서는 별도 DDL 실행 필요.
