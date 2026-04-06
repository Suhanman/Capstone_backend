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

#### 응답 구조 변경 — flat화

| 변경 전 접근 경로 | 변경 후 접근 경로 |
|----------------|----------------|
| `data.message` | `message` |

---

### 4. GET /api/inbox/{email_id}/recommendations

#### 응답 구조 변경 — 필드명 변경

| 변경 전 | 변경 후 |
|--------|--------|
| `data` (array) | `drafts` (array) |

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
