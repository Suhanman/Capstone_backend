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

**호출 흐름**
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

## 요청 필드명 규칙

아래 API는 요청 바디에 **camelCase** 필드명을 사용합니다.

| API | camelCase 필드 |
|-----|---------------|
| `POST /api/templates` | `categoryId`, `subjectTemplate`, `bodyTemplate` |
| `POST /api/calendar/events` | `startDatetime`, `endDatetime` |
