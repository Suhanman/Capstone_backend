# Admin IP 제한 설계 문서

## 1. 목적

`POST /api/auth/tokens` 로그인 시, `role = ADMIN` 계정은 **10.0.0.0/24** 대역에서만 접근을 허용한다.
그 외 IP에서의 Admin 로그인 시도는 **403 Forbidden**으로 거부한다.

---

## 2. 인프라 환경

```
클라이언트
    │
    ▼
MetalLB (L2 mode, External IP 할당)
    │  externalTrafficPolicy: Local  →  실제 클라이언트 IP 보존
    ▼
Nginx Ingress Controller v1.10.0
    │  use-forwarded-headers: false (기본값)
    │  → 클라이언트 X-Forwarded-For 헤더 신뢰 안 함 (스푸핑 방어)
    │  → X-Real-IP 에 실제 클라이언트 IP 주입
    ▼
Spring Boot Service Pod
    │  request.getRemoteAddr() = Nginx Pod 클러스터 IP  (사용 불가)
    │  request.getHeader("X-Real-IP") = 실제 클라이언트 IP  ✅
    ▼
IP 검증 로직
```

### 핵심 전제
- `externalTrafficPolicy: Local` 확인 완료 → 실제 클라이언트 IP 보존
- `use-forwarded-headers: false` → Nginx가 X-Real-IP를 직접 설정, 스푸핑 불가

---

## 3. 허용 IP 대역

| 항목 | 값 |
|------|----|
| 네트워크 주소 | 10.0.0.0 |
| 서브넷 마스크 | /24 (255.255.255.0) |
| 유효 범위 | 10.0.0.1 ~ 10.0.0.254 |
| 비교 방식 | 상위 24비트(3바이트) 일치 여부 |

---

## 4. 변경 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `controller/AuthController.java` | `HttpServletRequest` 파라미터 추가, IP 추출 후 Service 전달 |
| `service/AuthService.java` | `clientIp` 파라미터 추가, ADMIN role 시 서브넷 검증 호출 |
| `exception/AdminIpDeniedException.java` | 신규 - IP 차단 전용 예외 클래스 |
| `exception/GlobalExceptionHandler.java` | `AdminIpDeniedException` 403 처리 추가 |

---

## 5. 상세 설계

### 5-1. IP 추출 (AuthController)

```java
private String extractClientIp(HttpServletRequest request) {
    // Nginx Ingress가 주입하는 X-Real-IP 우선 사용
    String ip = request.getHeader("X-Real-IP");
    if (ip == null || ip.isBlank()) {
        // fallback: X-Forwarded-For 첫 번째 값
        String forwarded = request.getHeader("X-Forwarded-For");
        ip = (forwarded != null && !forwarded.isBlank())
            ? forwarded.split(",")[0].trim()
            : request.getRemoteAddr();
    }
    return ip;
}
```

### 5-2. 서브넷 검증 (AuthService)

```java
// role == ADMIN 인 경우에만 IP 검증 수행
private void validateAdminIp(User user, String clientIp) {
    if (user.getRole() != UserRole.ADMIN) return;

    try {
        byte[] client  = InetAddress.getByName(clientIp).getAddress();
        byte[] network = InetAddress.getByName("10.0.0.0").getAddress();
        // /24 = 상위 3바이트 일치 여부만 비교
        boolean allowed = client[0] == network[0]
                       && client[1] == network[1]
                       && client[2] == network[2];
        if (!allowed) {
            // 보안 감사 로그
            log.warn("[ADMIN IP DENIED] userId={} ip={}", user.getUserId(), clientIp);
            throw new AdminIpDeniedException(clientIp);
        }
    } catch (UnknownHostException e) {
        // IP 파싱 실패 → 안전하게 거부
        log.warn("[ADMIN IP PARSE FAIL] userId={} ip={}", user.getUserId(), clientIp);
        throw new AdminIpDeniedException(clientIp);
    }
}
```

### 5-3. login() 흐름 변경

```
login(LoginRequest, clientIp)
  ├─ 사용자 조회 (이메일)
  ├─ 비밀번호 검증
  ├─ isActive 확인
  ├─ [신규] validateAdminIp(user, clientIp)  ← ADMIN이면 IP 체크
  ├─ updateLastLogin()
  └─ JWT 발급
```

### 5-4. 예외 응답 (공통 응답 형식 준수)

```json
{
  "content_type": "application/json",
  "success": false,
  "result_code": 403,
  "result_req": "허용되지 않는 접근 IP입니다."
}
```

---

## 6. 로그인 시퀀스 다이어그램

```
Client          Nginx Ingress       AuthController        AuthService
  │                   │                   │                    │
  │── POST /api/auth/tokens ──────────>   │                    │
  │                   │  X-Real-IP 주입   │                    │
  │                   │──────────────>    │                    │
  │                   │              extractClientIp()         │
  │                   │                   │── login(req, ip) ─>│
  │                   │                   │                 findByEmail()
  │                   │                   │                 passwordCheck()
  │                   │                   │                 isActive check
  │                   │                   │                 validateAdminIp()
  │                   │                   │                    │
  │                   │                   │               [ADMIN + IP 불일치]
  │                   │                   │                 AdminIpDeniedException
  │                   │                   │                    │
  │<── 403 Forbidden ─────────────────────│                    │
  │                   │                   │               [허용]
  │                   │                   │               updateLastLogin()
  │                   │                   │               JWT 발급
  │<── 200 + access_token ────────────────│                    │
```

---

## 7. 보안 고려사항

| 항목 | 내용 |
|------|------|
| 스푸핑 방어 | `use-forwarded-headers: false` → Nginx가 X-Real-IP 직접 설정, 클라이언트 조작 불가 |
| IP 파싱 실패 | `UnknownHostException` 발생 시 안전하게 거부 (fail-closed) |
| 로그 기록 | 차단된 시도는 `WARN` 레벨로 userId, IP 기록 |
| USER role | IP 검증 로직 미적용, 기존 로그인 흐름 그대로 유지 |

---

## 8. 구현 순서

- [ ] `AdminIpDeniedException` 생성
- [ ] `GlobalExceptionHandler` 에 403 핸들러 추가
- [ ] `AuthService.login()` 에 `clientIp` 파라미터 및 `validateAdminIp()` 추가
- [ ] `AuthController.login()` 에 `HttpServletRequest` 및 IP 추출 추가
- [ ] 빌드 확인
