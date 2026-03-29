# 미구현 API 목록 및 TODO

> 기준: 2026-03-25 빌드 성공 후 코드 분석
> 담당 범위: 서비스 CRUD 팀 (`/api/business/**`, `/api/dashboard/**`, `/api/inbox/**`, `/api/templates/**`, `/api/calendar/**`, `/api/automations/**`, `/api/notifications/**`)

---

## ✅ 구현 완료 API

### /api/business
- [x] `GET    /api/business/profile` — 비즈니스 프로필 조회
- [x] `PUT    /api/business/profile` — 비즈니스 프로필 저장/수정
- [x] `GET    /api/business/resources/files` — 파일 목록 조회
- [x] `POST   /api/business/resources/files` — 파일 업로드
- [x] `DELETE /api/business/resources/files/{resourceId}` — 파일 삭제
- [x] `GET    /api/business/resources/faqs` — FAQ 목록 조회
- [x] `POST   /api/business/resources/faqs` — FAQ 생성
- [x] `PUT    /api/business/resources/faqs/{faqId}` — FAQ 수정
- [x] `DELETE /api/business/resources/faqs/{faqId}` — FAQ 삭제
- [x] `GET    /api/business/categories` — 카테고리 목록 조회
- [x] `POST   /api/business/categories` — 카테고리 생성
- [x] `DELETE /api/business/categories/{categoryId}` — 카테고리 삭제
- [x] `POST   /api/business/templates/regenerate` — 템플릿 재생성(LLM)

### /api/dashboard
- [x] `GET /api/dashboard/summary` — 대시보드 요약
- [x] `GET /api/dashboard/schedules` — 예정된 일정
- [x] `GET /api/dashboard/weekly-summary` — 주간 요약
- [x] `GET /api/dashboard/recent-emails` — 최근 이메일 목록

### /api/inbox
- [x] `GET  /api/inbox` — 수신함 목록 (페이징, status 필터)
- [x] `GET  /api/inbox/{emailId}` — 수신함 상세 (AI 초안 포함)
- [x] `POST /api/inbox/{emailId}/reply` — 답장 처리 (SKIP 액션)
- [x] `POST /api/inbox/{emailId}/calendar` — 일정 등록

### /api/templates
- [x] `GET    /api/templates` — 템플릿 목록 조회
- [x] `POST   /api/templates` — 템플릿 생성
- [x] `PUT    /api/templates/{templateId}` — 템플릿 수정
- [x] `DELETE /api/templates/{templateId}` — 템플릿 삭제

---

## ⚠️ 부분 구현 (내부 로직 TODO 남음)

### /api/inbox
- [ ] `POST /api/inbox/{emailId}/reply` — **SEND 액션**: Gmail API 실제 발송 미구현
  - 파일: `InboxService.java:119-123`
  - 현황: 상태 변경(PROCESSED/SENT)만 됨, 실제 메일 발송 없음
  - 담당: **Google OAuth 팀** (주석에 명시됨)
- [ ] `POST /api/inbox/{emailId}/reply` — **EDIT_SEND 액션**: Gmail API 실제 발송 미구현
  - 파일: `InboxService.java:125-133`
  - 현황: 상태 변경(PROCESSED/EDITED)만 됨, 실제 메일 발송 없음
  - 담당: **Google OAuth 팀** (주석에 명시됨)

---

## ❌ 미구현 API (Controller/Service 없음)

> API 설계서 기준 스펙 확인 필요. 아래는 `claude.md` 담당 범위 기준으로 누락된 영역.

### /api/calendar ✅ 구현 완료 (2026-03-25)
- [x] `GET    /api/calendar/events?start_date=&end_date=` — 기간 내 일정 목록
- [x] `GET    /api/calendar/events/{event_id}` — 일정 상세 조회
- [x] `POST   /api/calendar/events` — 수동 일정 추가 (source=MANUAL, status=CONFIRMED, is_calendar_added=true)
- [x] `PATCH  /api/calendar/events/{event_id}/confirm` — PENDING → CONFIRMED 상태 변경
- [x] `PUT    /api/calendar/events/{event_id}` — 일정 수정
- ※ Google Calendar API 실제 호출은 TODO 주석 처리 (Google OAuth 팀 담당)
- 생성 파일: `CalendarController.java`, `CalendarService.java`, `CalendarEventRequest.java`, `CalendarEventResponse.java`, `CalendarEventDetailResponse.java`
- 수정 파일: `CalendarEvent.java` (update 메서드 추가), `CalendarEventRepository.java` (findByPeriod 쿼리 추가)

### /api/automations ✅ 구현 완료 (2026-03-25)
- [x] `GET    /api/automations/rules` — 자동화 규칙 목록 조회
- [x] `POST   /api/automations/rules` — 규칙 생성 (category 없으면 자동 INSERT)
- [x] `PUT    /api/automations/rules/{rule_id}` — 규칙 수정
- [x] `DELETE /api/automations/rules/{rule_id}` — 규칙 삭제 (Categories 보존)
- [x] `PATCH  /api/automations/rules/{rule_id}/auto-send` — 자동 발송 토글
- [x] `PATCH  /api/automations/rules/{rule_id}/auto-calendar` — 일정 자동 등록 토글
- 생성 파일: `AutomationController.java`, `AutomationService.java`, `AutomationRule.java`, `AutomationRuleRepository.java`, `AutomationRuleRequest.java`, `AutomationToggleRequest.java`, `AutomationRuleResponse.java`
- 수정 파일: `CategoryRepository.java` (findByUser_UserIdAndCategoryName 추가)

### /api/notifications ✅ 구현 완료 (2026-03-25)
- [x] `GET   /api/notifications?is_read=false` — 알림 목록 (is_read 필터 선택)
- [x] `PATCH /api/notifications/{notification_id}/read` — 단일 알림 읽음 처리
- [x] `PATCH /api/notifications/read-all` — 전체 알림 일괄 읽음 처리
- 생성 파일: `NotificationController.java`, `NotificationService.java`, `Notification.java`, `NotificationRepository.java`, `NotificationResponse.java`

### /api/support-tickets ✅ 구현 완료 (2026-03-25)
- [x] `GET  /api/support-tickets?status=` — 문의 목록 (status 필터 선택)
- [x] `GET  /api/support-tickets/{ticket_id}` — 문의 상세 조회
- [x] `POST /api/support-tickets` — 문의 작성 (status=PENDING, admin_reply=null)
- 생성 파일: `SupportTicketController.java`, `SupportTicketService.java`, `SupportTicket.java`, `SupportTicketRepository.java`, `SupportTicketRequest.java`, `SupportTicketListResponse.java`, `SupportTicketDetailResponse.java`

---

## 📋 우선순위 제안

| 순위 | 항목 | 이유 |1
|------|------|------|
| 1 | `/api/calendar/**` | Entity·Repository 이미 존재, 연관 기능(inbox calendar 등록) 연결 필요 |
| 2 | `/api/notifications/**` | 사용자 알림은 다른 기능과 의존성 낮음, 독립 구현 가능 |
| 3 | `/api/automations/**` | 자동화 규칙은 이메일 처리 파이프라인과 연동 필요, 복잡도 높음 |
| - | Gmail SEND/EDIT_SEND | Google OAuth 팀 담당, 해당 팀에 전달 필요 |

---

## 🐛 빌드 수정 이력 (2026-03-25)

- `DashboardService.java:77` — `integration.getStatus()` → `integration.getSyncStatus()` (필드명 불일치 수정)
- `FileTextExtractor.java:44` — PDFBox 3.x API 변경 대응: `PDDocument.load()` → `Loader.loadPDF()` + import 추가

---

## 🔐 관리자(Admin) 기능 구현 계획 (2026-03-26)

> 기준: admin_prompt.md API 명세 + 기존 코드 구조 분석
> 패키지 전략: `controller/admin`, `service/admin`, `dto/request/admin`, `dto/response/admin/**`
> 인증 전략: SecurityConfig 이미 `/api/admin/**` → `hasRole("ADMIN")` 적용됨 → **수정 불필요**

---

### 📋 Phase 0. 사전 확인 사항

- [ ] `AutomationRule.java` 엔티티 필드 전체 확인 (PATCH 가능한 필드 범위 파악)
- [ ] `SupportTicket.java` 엔티티 필드 전체 확인 (status 필드, admin_reply 존재 여부)
- [ ] `Outbox.java` 엔티티 필드 전체 확인 → Operations/Jobs 매핑 적합성 검증
- [ ] `Email.java` 엔티티의 sender 도메인 파싱 가능 여부 확인 (도메인 분포 통계용)
- [ ] `BusinessProfile.java`의 industryType 필드 타입 확인 (사용자 검색 업종 필터용)

---

### 📋 Phase 1. DTO 생성

#### Request DTO (신규 생성)
- [ ] `dto/request/admin/AdminUserStatusUpdateRequest.java` — `{ "is_active": boolean }`
- [ ] `dto/request/admin/AdminAutomationRuleUpdateRequest.java` — 규칙 수정 가능 필드 (AutomationRule 필드 확인 후 결정)

#### Response DTO — Dashboard (신규 생성)
- [ ] `dto/response/admin/dashboard/AdminDashboardSummaryResponse.java` — 총 사용자수/활성/연동/오늘 처리 메일 수 등
- [ ] `dto/response/admin/dashboard/AdminEmailVolumeResponse.java` — 기간별 날짜+처리량 리스트
- [ ] `dto/response/admin/dashboard/AdminDomainDistributionResponse.java` — 도메인명+건수+비율 리스트
- [ ] `dto/response/admin/dashboard/AdminWeeklyTrendResponse.java` — 최근 7일 날짜+처리량 리스트

#### Response DTO — User (신규 생성)
- [ ] `dto/response/admin/user/AdminUserListResponse.java` — 사용자 목록 항목 (userId, name, email, industryType, isActive, createdAt)
- [ ] `dto/response/admin/user/AdminUserDetailResponse.java` — 사용자 상세 (기본정보 + 마지막 로그인 + 연동 요약)
- [ ] `dto/response/admin/user/AdminUserIntegrationResponse.java` — 연동 상태 (provider, connectedEmail, syncStatus, grantedScopes, tokenExpiresAt)

#### Response DTO — Template (신규 생성)
- [ ] `dto/response/admin/template/AdminTemplateListResponse.java` — 전체 템플릿 목록 (templateId, userId, title, categoryName, accuracyScore)
- [ ] `dto/response/admin/template/AdminTemplateCategoryStatResponse.java` — 카테고리명+템플릿 수+평균 정확도

#### Response DTO — Automation (신규 생성)
- [ ] `dto/response/admin/automation/AdminAutomationRuleListResponse.java` — 규칙 목록 항목
- [ ] `dto/response/admin/automation/AdminAutomationRuleDetailResponse.java` — 규칙 상세 전체 필드

#### Response DTO — Support Ticket (신규 생성)
- [ ] `dto/response/admin/support/AdminSupportTicketListResponse.java` — 문의 목록 (ticketId, userId, title, status, createdAt)
- [ ] `dto/response/admin/support/AdminSupportTicketDetailResponse.java` — 문의 상세 (내용 + adminReply 포함)

#### Response DTO — Operations/Jobs (신규 생성)
- [ ] `dto/response/admin/operation/AdminJobListResponse.java` — Outbox 작업 목록 (jobId, userId, status, createdAt)
- [ ] `dto/response/admin/operation/AdminJobSummaryResponse.java` — 상태별 건수 (READY/SENDING/FINISH/FAILED)
- [ ] `dto/response/admin/operation/AdminJobDetailResponse.java` — 특정 작업 상세
- [ ] `dto/response/admin/operation/AdminJobErrorResponse.java` — 실패 원인 로그 (FAILED 상태만 조회)

---

### 📋 Phase 2. Repository 수정 (기존 파일에 메서드 추가)

- [ ] `UserRepository.java` — 이름/이메일/업종 검색 쿼리 추가
  - `findByNameContainingOrEmailContaining(...)` 또는 `@Query` JPQL
  - BusinessProfile의 industryType으로 조인 검색 필요 시 `@Query` 사용
- [ ] `EmailRepository.java` — 통계 쿼리 추가
  - 기간별 처리량: `receivedAt` 기준 날짜 그룹핑 집계
  - 도메인 분포: sender 이메일에서 도메인 파싱 (`SUBSTRING_INDEX` 또는 애플리케이션 레벨 처리)
  - 주간 추이: 최근 7일 날짜별 건수
- [ ] `OutboxRepository.java` — 작업 관리 쿼리 추가
  - 상태별 건수 집계
  - FAILED 상태 에러 로그 필드 조회
  - READY/FAILED 상태만 삭제 가능하도록 서비스 레벨 검증 지원
- [ ] `TemplateRepository.java` — 관리자용 전체 조회 추가
  - 전체 사용자 템플릿 목록 (기존은 userId 필터)
  - 카테고리별 통계: `@Query` GROUP BY categoryId
- [ ] `AutomationRuleRepository.java` — 전체 조회 추가
  - 전체 사용자 규칙 목록 (기존은 userId 필터)
- [ ] `SupportTicketRepository.java` — 전체 + 상태 필터 조회 추가

---

### 📋 Phase 3. Service 생성 (신규 파일)

- [ ] `service/admin/AdminDashboardService.java`
  - `getSummary()` — User/Email/Integration 집계
  - `getEmailVolume(LocalDate from, LocalDate to)` — 기간별 통계
  - `getDomainDistribution()` — 도메인별 분포
  - `getWeeklyTrend()` — 최근 7일 추이

- [ ] `service/admin/AdminUserService.java`
  - `getUsers(String name, String email, String industryType)` — 검색 포함 목록
  - `getUserDetail(Long userId)` — 상세 조회
  - `getUserIntegration(Long userId)` — 연동 상태 조회
  - `updateUserStatus(Long userId, boolean isActive)` — 활성/비활성 전환
  - `deleteUserIntegration(Long userId)` — 구글 연동 강제 해제

- [ ] `service/admin/AdminTemplateService.java`
  - `getAllTemplates()` — 전체 템플릿 목록
  - `getTemplateCategoryStats()` — 카테고리별 통계

- [ ] `service/admin/AdminAutomationService.java`
  - `getAllRules()` — 전체 규칙 목록
  - `getRuleDetail(Long ruleId)` — 규칙 상세
  - `updateRule(Long ruleId, AdminAutomationRuleUpdateRequest req)` — 규칙 수정
  - `deleteRule(Long ruleId)` — 규칙 삭제

- [ ] `service/admin/AdminSupportTicketService.java`
  - `getTickets(String status)` — 전체 + 상태 필터
  - `getTicketDetail(Long ticketId)` — 상세 조회

- [ ] `service/admin/AdminOperationService.java`
  - `getJobs(String status)` — 작업 목록 + 상태 필터
  - `getJobsSummary()` — 상태별 건수
  - `getJobDetail(Long jobId)` — 작업 상세
  - `getJobError(Long jobId)` — 실패 원인 로그 (FAILED 상태만)
  - `deleteJob(Long jobId)` — READY/FAILED 상태만 삭제 허용

---

### 📋 Phase 4. Controller 생성 (신규 파일)

- [ ] `controller/admin/AdminDashboardController.java`
  - `GET /api/admin/dashboard/summary`
  - `GET /api/admin/dashboard/email-volume`
  - `GET /api/admin/dashboard/domain-distribution`
  - `GET /api/admin/dashboard/weekly-trend`

- [ ] `controller/admin/AdminUserController.java`
  - `GET    /api/admin/users` — 목록 + 검색 (query params: name, email, industry_type)
  - `GET    /api/admin/users/{user_id}`
  - `GET    /api/admin/users/{user_id}/integration`
  - `PATCH  /api/admin/users/{user_id}/status`
  - `DELETE /api/admin/users/{user_id}/integration`

- [ ] `controller/admin/AdminTemplateController.java`
  - `GET /api/admin/templates`
  - `GET /api/admin/templates/statistics/by-category`

- [ ] `controller/admin/AdminAutomationController.java`
  - `GET    /api/admin/automations/rules`
  - `GET    /api/admin/automations/rules/{rule_id}`
  - `PATCH  /api/admin/automations/rules/{rule_id}`
  - `DELETE /api/admin/automations/rules/{rule_id}`

- [ ] `controller/admin/AdminSupportTicketController.java`
  - `GET /api/admin/support-tickets` — 목록 + 상태 필터 (query param: status)
  - `GET /api/admin/support-tickets/{ticket_id}`

- [ ] `controller/admin/AdminOperationController.java`
  - `GET    /api/admin/operations/jobs` — 목록 + 상태 필터
  - `GET    /api/admin/operations/jobs/summary`
  - `GET    /api/admin/operations/jobs/{job_id}`
  - `GET    /api/admin/operations/jobs/{job_id}/error`
  - `DELETE /api/admin/operations/jobs/{job_id}`

---

### 📋 Phase 5. 예외 처리 검토

- [ ] `GlobalExceptionHandler.java` 검토 — 기존 `ResourceNotFoundException(404)` 재사용 가능 여부 확인
  - 신규 예외 추가 여부: `AdminOperationNotAllowedException` (READY/FAILED 외 작업 삭제 시도 등)
  - 기존 예외로 커버 가능하면 신규 클래스 생성 생략

---

### 📋 Phase 6. 빌드 검증

- [ ] `mvn compile` 성공 확인
- [ ] 각 Controller endpoint 매핑 정상 등록 확인
- [ ] 관리자 인증 흐름: ADMIN role JWT → `/api/admin/**` 접근 허용 확인

---

### 구현 우선순위

| 순위 | Phase | 근거 |
|------|-------|------|
| 1 | Phase 0 (사전 확인) | 필드 미확인 상태로 DTO 설계 불가 |
| 2 | Phase 1 (DTO) | 인터페이스 계약 선확정 후 내부 구현 |
| 3 | Phase 2 (Repository) | Service 로직의 데이터 접근 기반 |
| 4 | Phase 3 (Service) | 비즈니스 로직 핵심 |
| 5 | Phase 4 (Controller) | 라우팅 연결 |
| 6 | Phase 5~6 (예외처리/검증) | 마무리 품질 확보 |

---

## 🔀 리팩토링: 부분 연동 (Granular Consent) 적용 (2026-03-28)

> 기준: refactoring_prompt.md
> 목표: `is_connected` 단일 관리 → Gmail(필수) / Calendar(선택) 세분화

### 수정 대상 파일 및 변경 요약

#### 영역 1: Integrations — Google OAuth 팀 담당

- [x] **`Integration.java`** (Entity)
  - `is_gmail_connected` boolean 컬럼 추가
  - `is_calendar_connected` boolean 컬럼 추가
  - `disconnectCalendar()` 메서드 추가
  - **DB 마이그레이션**: 완료 (사용자가 직접 실행)

- [x] **`CallbackResponse.java`** (신규 DTO)
  - `success`, `is_gmail_connected`, `is_calendar_connected` 필드

- [x] **`IntegrationResponse.java`** (DTO 수정)
  - `is_gmail_connected`, `is_calendar_connected` 필드 추가

- [x] **`DeleteIntegrationRequest.java`** (신규 DTO)
  - `target_service` 필드 (ALL / CALENDAR)

- [x] **`IntegrationRepository.java`** (Repository 수정)
  - `countByIsGmailConnectedTrue()` 추가
  - `countByIsCalendarConnectedTrue()` 추가

- [x] **`GoogleOAuthService.java`** (Service 수정)
  - `handleCallback()`: Gmail scope 없으면 예외, Calendar scope 없으면 `is_calendar_connected=false`로 저장. 반환 타입 `CallbackResponse`로 변경
  - `deleteIntegration()`: `targetService` 파라미터 추가. ALL이면 레코드 삭제, CALENDAR면 `disconnectCalendar()`

- [x] **`IntegrationController.java`** (Controller 수정)
  - `handleCallback()` 반환 타입 `CallbackResponse`
  - `deleteIntegration()` `@RequestBody DeleteIntegrationRequest` 추가

#### 영역 2: Calendar 서비스 — 서비스 CRUD 팀 담당

- [x] **`CalendarNotConnectedException.java`** (신규 Exception)
  - 캘린더 권한 없는 사용자 호출 시 발생

- [x] **`GlobalExceptionHandler.java`** (수정)
  - `CalendarNotConnectedException` 핸들러: HTTP 403 + 새 API 공통 응답 규격 (`ApiErrorResponse` inner record)

- [x] **`CalendarService.java`** (수정)
  - `createEvent()` 시작 전 `is_calendar_connected` 검증

- [x] **`InboxService.java`** (수정)
  - `processCalendar()` ADD 액션 전 `is_calendar_connected` 검증

#### 영역 3: 관리자 대시보드 — 서비스 CRUD 팀 담당

- [x] **`AdminDashboardSummaryResponse.java`** (DTO 수정)
  - `connected_users` → `gmail_connected_users` + `calendar_connected_users` 분리

- [x] **`AdminDashboardService.java`** (Service 수정)
  - `getSummary()`: 새 Repository 메서드로 각각 집계

### 구현 순서
1. Entity → Repository → Exception → Handler
2. Service (GoogleOAuthService) + DTO + Controller (Integrations)
3. CalendarService + InboxService
4. AdminDashboardSummaryResponse + AdminDashboardService
