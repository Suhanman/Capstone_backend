# API 변경 사항 (프론트엔드 업데이트 필요)

> 작성일: 2026-04-03
> 관련 작업: 첨부파일 On-demand 처리 방식 구현 + 전체 응답 구조 flat화

---

## ⚠️ 공통 구조 변경 — 전체 API `data` 래퍼 제거

기존 일부 API에 존재하던 `data` 래퍼 객체가 제거되었습니다.
모든 API의 비즈니스 데이터 필드는 이제 최상위 레벨에 flat하게 위치합니다.

**변경 전 (data 래퍼 있음)**
```json
{ "success": true, "result_code": 200, "result_req": "", "data": { "items": [...] } }
```

**변경 후 (flat)**
```json
{ "success": true, "result_code": 200, "result_req": "", "items": [...] }
```

---

## 변경된 API 목록

### 1. GET /api/inbox

#### 응답 구조 변경 — flat화

| 변경 전 접근 경로 | 변경 후 접근 경로 |
|----------------|----------------|
| `data.total_elements` | `total_elements` |
| `data.content` | `content` |

#### 신규 필드 — `content` 배열 아이템에 추가

| 필드 | 타입 | 설명 |
|------|------|------|
| `has_attachments` | boolean | 첨부파일 포함 여부 |

**변경 후 응답 예시**
```json
{
  "content_type": "application/json",
  "success": true,
  "result_code": 200,
  "result_req": "",
  "total_elements": 125,
  "content": [
    {
      "email_id": 501,
      "sender_name": "박민수",
      "subject": "엔터프라이즈 플랜 가격 문의",
      "received_at": "2026-03-12T10:23:00",
      "status": "PENDING_REVIEW",
      "category_name": "가격문의",
      "schedule_detected": true,
      "has_attachments": true
    }
  ]
}
```

---

### 2. GET /api/inbox/{email_id}

#### 응답 구조 변경 — flat화

| 변경 전 접근 경로 | 변경 후 접근 경로 |
|----------------|----------------|
| `data.email_info` | `email_info` |
| `data.ai_analysis` | `ai_analysis` |
| `data.draft_reply` | `draft_reply` |

#### 신규 필드 — `ai_analysis` 내부에 추가

| 필드 | 타입 | 설명 |
|------|------|------|
| `summary` | string | AI가 생성한 이메일 요약 텍스트 |

#### 신규 필드 — `email_info` 내부에 추가

| 필드 | 타입 | 설명 |
|------|------|------|
| `has_attachments` | boolean | 첨부파일 포함 여부 |
| `attachments` | array | 첨부파일 목록 (없으면 빈 배열 `[]`) |
| `attachments[].attachment_id` | int | 다운로드 API 호출 시 사용하는 첨부파일 번호 (1부터 시작) |
| `attachments[].file_name` | string | 원본 파일명 |
| `attachments[].content_type` | string | MIME 타입 (예: `application/pdf`) |
| `attachments[].size` | long | 파일 크기 (Byte 단위) |

**변경 후 응답 예시**
```json
{
  "content_type": "application/json",
  "success": true,
  "result_code": 200,
  "result_req": "",
  "email_info": {
    "email_id": 501,
    "sender_name": "박민수",
    "subject": "엔터프라이즈 플랜 가격 문의",
    "body": "...",
    "received_at": "2026-03-12T10:23:00",
    "has_attachments": true,
    "attachments": [
      {
        "attachment_id": 1,
        "file_name": "엔터프라이즈_도입제안서.pdf",
        "content_type": "application/pdf",
        "size": 2048576
      }
    ]
  },
  "ai_analysis": { ... },
  "draft_reply": { ... }
}
```

---

### 3. POST /api/inbox/{email_id}/regenerate

#### 요청 바디

```json
{ "previous_draft": "기존 초안 텍스트" }
```

#### 응답 구조 변경 — flat화

| 변경 전 접근 경로 | 변경 후 접근 경로 |
|----------------|----------------|
| `data.message` | `message` |

---

### 4. GET /api/inbox/{email_id}/recommendations

쿼리 파라미터: `?topK=3`

#### 응답 구조 변경 — 필드명 변경

| 변경 전 | 변경 후 |
|--------|--------|
| `data` (array) | `drafts` (array) |

#### `drafts` 배열 아이템 필드

```json
{
  "result_code": 200, "result_req": "", "success": true,
  "drafts": [
    { "draft_id": 1, "subject": "...", "body": "...", "similarity": 0.95, "email_id": 100 }
  ]
}
```

---

### 5. GET /api/dashboard/schedules

#### 응답 구조 변경 — 필드명 변경

| 변경 전 | 변경 후 |
|--------|--------|
| `data` (array) | `schedules` (array) |

---

### 6. GET /api/dashboard/recent-emails

#### 응답 구조 변경 — 필드명 변경

| 변경 전 | 변경 후 |
|--------|--------|
| `data` (array) | `emails` (array) |

---

### 7. GET /api/automations/rules

#### 응답 구조 변경 — 필드명 변경

| 변경 전 | 변경 후 |
|--------|--------|
| `data` (array) | `rules` (array) |

---

### 8. GET /api/notifications

#### 응답 구조 변경 — 필드명 변경

| 변경 전 | 변경 후 |
|--------|--------|
| `data` (array) | `notifications` (array) |

---

### 9. GET /api/templates

#### 응답 구조 변경 — 필드명 변경

| 변경 전 | 변경 후 |
|--------|--------|
| `data` (array) | `templates` (array) |

---

### 10. GET /api/calendar/events

#### 응답 구조 변경 — 필드명 변경

| 변경 전 | 변경 후 |
|--------|--------|
| `data` (array) | `events` (array) |

#### 신규 필드 — `events` 배열 아이템에 추가 (2026-04-06)

| 필드 | 타입 | 설명 |
|------|------|------|
| `event_type` | string | 일정 유형 (`meeting` / `video` / `call` / `deadline`), nullable |
| `location` | string | 장소 또는 회의 링크, nullable |

**변경 후 응답 예시**
```json
{
  "content_type": "application/json",
  "success": true,
  "result_code": 200,
  "result_req": "",
  "events": [
    {
      "event_id": 12,
      "title": "그린에너지 파트너십 미팅",
      "start_datetime": "2026-04-10T10:00:00",
      "end_datetime": "2026-04-10T11:30:00",
      "event_type": "meeting",
      "location": "본사 3층 회의실 A",
      "source": "EMAIL",
      "status": "PENDING",
      "is_calendar_added": false,
      "created_at": "2026-04-04T12:00:00"
    }
  ]
}
```

---

### 11-1. GET /api/calendar/events/{event_id} — 응답 확장 (2026-04-06)

#### 신규 필드 추가

| 필드 | 타입 | 설명 |
|------|------|------|
| `event_type` | string | 일정 유형 (`meeting` / `video` / `call` / `deadline`), nullable |
| `location` | string | 장소 또는 회의 링크, nullable |
| `notes` | string | 메모 / 상세 내용, nullable |
| `email_sender_name` | string | 원본 이메일 발신자명 (이메일 감지 일정만 존재), nullable |
| `email_subject` | string | 원본 이메일 제목 (이메일 감지 일정만 존재), nullable |

**변경 후 응답 예시**
```json
{
  "content_type": "application/json",
  "success": true,
  "result_code": 200,
  "result_req": "",
  "event_id": 12,
  "title": "그린에너지 파트너십 미팅",
  "start_datetime": "2026-04-10T10:00:00",
  "end_datetime": "2026-04-10T11:30:00",
  "event_type": "meeting",
  "location": "본사 3층 회의실 A",
  "notes": "전략적 파트너십 논의",
  "source": "EMAIL",
  "status": "PENDING",
  "is_calendar_added": false,
  "email_id": 301,
  "email_sender_name": "최영호",
  "email_subject": "3월 파트너십 미팅 요청",
  "created_at": "2026-04-04T12:00:00",
  "updated_at": "2026-04-04T12:30:00"
}
```

---

### 11-2. POST /api/calendar/events · PUT /api/calendar/events/{event_id} — 요청 확장 (2026-04-06)

#### 신규 요청 필드 추가

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `eventType` | string | 선택 | 일정 유형 (`meeting` / `video` / `call` / `deadline`) |
| `location` | string | 선택 | 장소 또는 회의 링크 |
| `notes` | string | 선택 | 메모 / 상세 내용 |

**요청 예시**
```json
{
  "title": "그린에너지 파트너십 미팅",
  "startDatetime": "2026-04-10T10:00:00",
  "endDatetime": "2026-04-10T11:30:00",
  "eventType": "meeting",
  "location": "본사 3층 회의실 A",
  "notes": "전략적 파트너십 논의"
}
```

> 요청 필드는 camelCase, 응답 필드는 snake_case 입니다.

---

### 11. GET /api/business/resources/files

#### 응답 구조 변경 — 필드명 변경

| 변경 전 | 변경 후 |
|--------|--------|
| `data` (array) | `resources` (array) |

---

### 12. GET /api/business/resources/faqs

#### 응답 구조 변경 — 필드명 변경

| 변경 전 | 변경 후 |
|--------|--------|
| `data` (array) | `faqs` (array) |

---

### 13. GET /api/business/categories

#### 응답 구조 변경 — 필드명 변경

| 변경 전 | 변경 후 |
|--------|--------|
| `data` (array) | `categories` (array) |

---

### 14. GET /api/support-tickets

#### 응답 구조 변경 — 필드명 변경

| 변경 전 | 변경 후 |
|--------|--------|
| `data` (array) | `tickets` (array) |

---

## 신규 API — 비밀번호 찾기 (2026-04-06)

### POST /api/auth/password-reset

비로그인 상태에서 이름 + 이메일로 본인 확인 후 새 비밀번호로 변경합니다.

**인증**: 불필요 (JWT 없이 호출 가능)

**요청**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `name` | string | ✅ | 가입 시 등록한 이름 |
| `email` | string | ✅ | 가입 시 등록한 이메일 |
| `new_password` | string | ✅ | 새 비밀번호 (8자 이상) |

```json
{
  "name": "홍길동",
  "email": "hong@example.com",
  "new_password": "newpassword123"
}
```

**성공 응답 (200)**

```json
{
  "content_type": "application/json",
  "success": true,
  "result_code": 200,
  "result_req": ""
}
```

**실패 응답**

| 상황 | result_code | result_req |
|------|-------------|------------|
| 이름/이메일 불일치 | 400 | 이름 또는 이메일이 올바르지 않습니다. |
| 비활성화된 계정 | 400 | 비활성화된 계정입니다. |
| 유효성 검사 실패 | 400 | (필드별 오류 메시지) |

> 이름과 이메일이 모두 일치해야 변경됩니다. 보안상 어느 쪽이 틀렸는지 구분하지 않습니다.

---

## 신규 API — 첨부파일 다운로드

### GET /api/inbox/{email_id}/attachments/{attachment_id}

첨부파일은 서버에 저장되지 않으며, 클릭 시점에 Gmail에서 실시간으로 가져와 즉시 반환합니다.

| 파라미터 | 위치 | 타입 | 설명 |
|---------|------|------|------|
| `email_id` | Path | int | 이메일 고유 ID |
| `attachment_id` | Path | int | `email_info.attachments[].attachment_id` 값 |

**응답**: JSON이 아닌 **파일 바이너리 스트림**

| 헤더 | 예시 |
|------|------|
| `Content-Type` | `application/pdf` |
| `Content-Disposition` | `attachment; filename="파일명.pdf"; filename*=UTF-8''...` |

**처리 권장**
- `content_type`이 `application/pdf` 또는 `image/*` → 팝업/새 창 미리보기
- 그 외 → 로컬 다운로드

**호출 흐름**/resume
```
1. GET /api/inbox/501          → email_info.attachments[0].attachment_id = 1 확인
2. 사용자가 파일명 클릭
3. GET /api/inbox/501/attachments/1  → 파일 스트림 수신
```

---

## DB 변경사항

- `Users` 테이블에 `onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE` 컬럼 추가

```sql
ALTER TABLE Users ADD COLUMN onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE;
```

---

## 신규 추가된 API

### 온보딩

#### GET /api/onboarding/status

```json
{ "result_code": 200, "result_req": "", "success": true, "onboarding_completed": false }
```

#### POST /api/onboarding/complete

```json
{ "result_code": 200, "result_req": "", "success": true }
```

#### POST /api/business/templates/generate-initial

**요청**
```json
{
  "industry_type": "SaaS",
  "email_tone": "NEUTRAL",
  "company_description": "...",
  "category_ids": [1, 2, 3],
  "faq_ids": [1, 2],
  "resource_ids": [1]
}
```

**응답**
```json
{ "result_code": 200, "result_req": "", "success": true, "status": "PROCESSING", "processing_count": 3 }
```

---

### 캘린더

#### DELETE /api/calendar/events/{event_id}

```json
{ "result_code": 200, "result_req": "", "success": true }
```

---

## 추가 변경된 API

### POST /api/automations/rules — `keywords` 필드 제거

요청 바디에서 `keywords` 필드가 제거되었습니다.

### PUT /api/automations/rules/{rule_id} — `keywords` 필드 제거

요청 바디에서 `keywords` 필드가 제거되었습니다.

---

---

## 수신함 API 응답 필드 보강 (2026-04-08)

### GET /api/inbox

#### 신규 필드 — `content` 배열 아이템에 추가

| 필드 | 타입 | 설명 |
|------|------|------|
| `draft_status` | string \| null | DraftReply 없으면 `null`, 있으면 `PENDING_REVIEW` / `EDITED` / `SENT` / `SKIPPED` |

---

### GET /api/inbox/{emailId}

#### 신규 필드 — `email_info` 내부에 추가

| 필드 | 타입 | 설명 |
|------|------|------|
| `sender_email` | string | 발신자 이메일 주소 |

#### 신규 필드 — `ai_analysis` 내부에 추가

`schedule` 객체가 항상 포함됩니다. CalendarEvent가 없으면 `has_schedule: false`, 나머지 필드는 `null`입니다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `schedule.has_schedule` | boolean | 감지된 일정 존재 여부 |
| `schedule.title` | string \| null | 일정 제목 |
| `schedule.date` | string \| null | 일정 날짜 (`YYYY-MM-DD`) |
| `schedule.start_time` | string \| null | 시작 시간 (`HH:MM`) |
| `schedule.end_time` | string \| null | 종료 시간 (`HH:MM`) |
| `schedule.location` | string \| null | 장소 |
| `schedule.participants` | array \| null | 참석자 목록 |

**응답 예시 — 일정 있음**
```json
{
  "ai_analysis": {
    "domain": "...",
    "intent": "...",
    "summary": "...",
    "schedule": {
      "has_schedule": true,
      "title": "미팅 제목",
      "date": "2026-04-10",
      "start_time": "14:00",
      "end_time": "15:00",
      "location": "본사 3층",
      "participants": null
    }
  }
}
```

**응답 예시 — 일정 없음**
```json
{
  "ai_analysis": {
    "schedule": {
      "has_schedule": false,
      "title": null,
      "date": null,
      "start_time": null,
      "end_time": null,
      "location": null,
      "participants": null
    }
  }
}
```

---

## 요청 필드명 규칙

아래 API는 요청 바디에 **camelCase** 필드명을 사용합니다.

| API | camelCase 필드 |
|-----|---------------|
| `POST /api/templates` | `categoryId`, `subjectTemplate`, `bodyTemplate` |
| `POST /api/calendar/events` | `startDatetime`, `endDatetime` |

---

## [2026-04-21] 템플릿/자동화 관리 페이지 API 추가

### DB 변경
```sql
-- automation_rules 테이블 컬럼 추가
ALTER TABLE automation_rules
ADD COLUMN name VARCHAR(255) NULL,
ADD COLUMN trigger_condition TEXT NULL,
ADD COLUMN action_description VARCHAR(500) NULL;

-- templates 테이블 컬럼 추가
ALTER TABLE templates
ADD COLUMN use_count INT DEFAULT 0,
ADD COLUMN user_count INT DEFAULT 0;
```

-- industry는 BusinessProfiles.industry_type JOIN으로 조회 (컬럼 추가 없음)
-- quality는 별도 컬럼 없이 accuracy_score 기반 계산
-- 0.8 이상 → 높음, 0.5~0.8 → 보통, 0.5 미만 → 낮음

### API 변경사항
- GET /api/admin/automations/rules
  응답에 name, trigger, action, category(이름), status("활성"/"비활성"), updated_at 필드 추가
- GET /api/admin/automations/rules/{rule_id}
  응답에 동일 필드 추가
- POST /api/admin/automations/rules
  요청에 name, trigger_condition, action_description 필드 추가
- PATCH /api/admin/automations/rules/{rule_id}
  요청에 name, trigger_condition, action_description 필드 추가
- GET /api/admin/templates
  응답에 category(이름), industry(BusinessProfiles.industry_type),
  use_count, user_count, generated_at, quality 필드 추가
  quality = accuracy_score 기반 계산 (0.8이상=높음, 0.5~0.8=보통, 0.5미만=낮음)
- GET /api/admin/templates/summary 신규 추가
  응답: { total_templates, top_category, top_category_usage_count,
          active_rule_count, auto_send_rule_count }

---

## [2026-04-28] 비밀번호 재설정 API 변경 — 이메일 인증 코드 방식으로 전환

### ⚠️ 삭제된 API

| 메서드 | 경로 |
|--------|------|
| `POST` | `/api/auth/password-reset` |

이름+이메일 입력 즉시 비밀번호를 변경하는 방식이 제거되었습니다.

---

### 신규 API — Step 1: 인증 코드 발송

#### POST /api/auth/password-reset/code

**인증**: 불필요 (JWT 없이 호출 가능)

**요청**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `name` | string | ✅ | 가입 시 등록한 이름 |
| `email` | string | ✅ | 가입 시 등록한 이메일 |

```json
{
  "name": "홍길동",
  "email": "hong@example.com"
}
```

**성공 응답 (200)** — 이름·이메일이 일치하면 해당 이메일로 6자리 인증 코드 발송

```json
{
  "content_type": "application/json",
  "success": true,
  "result_code": 200,
  "result_req": ""
}
```

**실패 응답**

| 상황 | result_code | result_req |
|------|-------------|------------|
| 이름/이메일 불일치 또는 비활성 계정 | 400 | 이름 또는 이메일이 올바르지 않습니다. |
| 유효성 검사 실패 | 400 | 입력값이 올바르지 않습니다: ... |
| Gmail SMTP 발송 실패 | 503 | 이메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요. |

> 보안상 이름과 이메일 중 어느 쪽이 틀렸는지 구분하지 않습니다.

---

### 신규 API — Step 2: 인증 코드 검증 및 비밀번호 재설정

#### POST /api/auth/password-reset/verify

**인증**: 불필요 (JWT 없이 호출 가능)

**요청**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | string | ✅ | Step 1에서 사용한 이메일 |
| `code` | string | ✅ | 수신한 6자리 인증 코드 |
| `new_password` | string | ✅ | 새 비밀번호 (8자 이상) |

```json
{
  "email": "hong@example.com",
  "code": "483920",
  "new_password": "newpassword123"
}
```

**성공 응답 (200)**

```json
{
  "content_type": "application/json",
  "success": true,
  "result_code": 200,
  "result_req": ""
}
```

**실패 응답**

| 상황 | result_code | result_req |
|------|-------------|------------|
| 코드 미발송 또는 만료 (5분 초과) | 400 | 인증 코드가 존재하지 않습니다. 다시 요청해 주세요. / 인증 코드가 만료되었습니다. 다시 요청해 주세요. |
| 코드 불일치 | 400 | 인증 코드가 올바르지 않습니다. |
| 유효성 검사 실패 | 400 | 입력값이 올바르지 않습니다: ... |

> 인증 코드는 검증 성공 즉시 삭제되어 재사용할 수 없습니다. 코드 유효 시간은 5분입니다.

---

### 프론트엔드 변경 플로우

```
1. 사용자가 이름 + 이메일 입력
   → POST /api/auth/password-reset/code

2. 이메일로 수신한 6자리 코드 + 새 비밀번호 입력
   → POST /api/auth/password-reset/verify

3. 성공 시 로그인 페이지로 이동
```

---

## [2026-04-28] Google 회원가입 플로우 추가 및 Gmail 중복 방지

### DB 변경

```sql
-- integrations.connected_email에 UNIQUE 제약 추가
-- (Gmail 계정 하나 = 서비스 계정 하나 원칙 강제)
-- ddl-auto: update 환경에서 서버 재시작 시 자동 반영
ALTER TABLE integrations ADD UNIQUE KEY (connected_email);
```

> 기존 데이터 중 `connected_email` 중복 행이 있으면 서버 기동 실패. 운영 DB 적용 전 중복 여부 확인 필요.

---

### 신규 API — Google 회원가입 Step 1: OAuth URL 발급

#### GET /api/auth/google/signup-url

**인증**: 불필요 (비로그인 상태에서 호출)

**응답 (200)**

```json
{
  "content_type": "application/json",
  "success": true,
  "result_code": 200,
  "result_req": "",
  "authorization_url": "https://accounts.google.com/o/oauth2/..."
}
```

프론트엔드는 `authorization_url`로 사용자를 이동시켜 Google 동의 화면을 표시합니다.

---

### 신규 API — Google 회원가입 Step 2: 비밀번호 입력 후 계정 생성

#### POST /api/auth/google/signup

**인증**: 불필요

**요청**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `temp_token` | string | ✅ | OAuth 콜백 후 받은 임시 토큰 (유효 시간 10분) |
| `password` | string | ✅ | 서비스 전용 비밀번호 (8자 이상) |

```json
{
  "temp_token": "550e8400-e29b-41d4-a716-446655440000",
  "password": "mypassword123"
}
```

**성공 응답 (200)**

```json
{
  "content_type": "application/json",
  "success": true,
  "result_code": 200,
  "result_req": "",
  "access_token": "<JWT>",
  "token_type": "Bearer",
  "expires_in": 86400
}
```

**실패 응답**

| 상황 | result_code | result_req |
|------|-------------|------------|
| temp_token 만료 또는 없음 | 400 | 회원가입 세션이 만료되었습니다. 다시 시도해 주세요. |
| 이미 가입된 이메일 | 400 | 이미 가입된 이메일입니다. |
| 유효성 검사 실패 | 400 | 입력값이 올바르지 않습니다: ... |

---

### 변경된 API — GET /api/integrations/google/callback (OAuth 콜백)

기존에는 로그인한 유저의 Gmail 연동만 처리했으나, **회원가입 모드(SIGNUP)도 처리**하도록 확장되었습니다. 콜백 URL은 동일하며 state JWT의 mode 값으로 내부 분기합니다.

#### 콜백 결과별 redirect URL

| 상황 | redirect 대상 |
|------|--------------|
| 기존 로그인 유저 연동 완료 | `{frontendBaseUrl}/app/settings?tab=email&google_oauth=success&gmail_connected=true&calendar_connected=true/false` |
| 기존 계정 자동 로그인 (Gmail 중복) | `{frontendBaseUrl}/app/dashboard?google_oauth=auto_login&token=<JWT>` |
| 신규 유저 (비밀번호 입력 필요) | `{frontendBaseUrl}/auth/google/register?temp_token=...&email=...&name=...` |
| 오류 발생 | `{frontendBaseUrl}/auth/google/register?error=true&message=...` (signup) 또는 `{frontendBaseUrl}/app/settings?tab=email&google_oauth=error&message=...` (연동) |

---

### 프론트엔드 구현 가이드

#### Google 회원가입 플로우 (신규)

```
1. "구글로 시작하기" 버튼 클릭
   → GET /api/auth/google/signup-url → authorization_url로 이동

2. Google 동의 후 콜백
   [신규 유저] → /auth/google/register?temp_token=...&email=...&name=...
     - email, name은 read-only로 자동 채워줌
     - 사용자가 비밀번호만 입력
     → POST /api/auth/google/signup { temp_token, password }
     → 성공 시 JWT 수령 후 로그인 상태 진입

   [기존 계정] → /app/dashboard?google_oauth=auto_login&token=<JWT>
     - token을 저장하여 자동 로그인 처리

3. temp_token 유효 시간: 10분
```

#### 환경 변수 (선택 — 기본값 있음)

| 환경 변수 | 기본값 | 설명 |
|----------|--------|------|
| `APP_FRONTEND_SIGNUP_SUCCESS_PATH` | `/app/dashboard` | 자동 로그인 후 redirect 경로 |
| `APP_FRONTEND_SIGNUP_REGISTER_PATH` | `/auth/google/register` | 신규 유저 회원가입 페이지 경로 |
