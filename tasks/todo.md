# 응답 형식 BaseResponse 통일 작업

## 목표
모든 API 응답을 CLAUDE.md 공통 응답 형식으로 통일
- content_type, success, result_code, result_req 4개 필드 필수 포함
- 비즈니스 데이터는 공통 필드와 같은 레벨(Flat 구조)에 추가
- 임의의 중첩 data 객체 금지

## 체크리스트

### Phase 1: BaseResponse 재설계 ✅
- [x] BaseResponse.java — concrete 클래스, success 추가, content_type="application/json", 기본/오류 생성자
- [x] SuccessResponse.java — 삭제
- [x] DeleteUserResponse.java — 삭제
- [x] UserService — deleteMe/changePassword 반환 타입 → BaseResponse
- [x] GoogleOAuthService — deleteIntegration 반환 타입 → BaseResponse, 파라미터 정리

### Phase 2: 비즈니스 DTO 리팩토링 ✅
- [x] BusinessProfileResponse — Flat, extends BaseResponse
- [x] BusinessResourceResponse — extends BaseResponse1
- [x] FaqResponse — extends BaseResponse
- [x] CategoryResponse — extends BaseResponse
- [x] TemplateRegenerateResponse — 플래튼화, extends BaseResponse
- [x] AutomationRuleResponse.ListResponse — extends BaseResponse
- [x] NotificationResponse — ListResponse extends BaseResponse, SimpleResponse 삭제
- [x] InboxListResponse — InboxPage 래퍼 제거, Flat, extends BaseResponse
- [x] InboxDetailResponse — DetailData 래퍼 제거, Flat, extends BaseResponse
- [x] SummaryResponse — SummaryData 래퍼 제거, Flat, extends BaseResponse
- [x] ScheduleResponse — extends BaseResponse
- [x] WeeklySummaryResponse — WeeklyData 래퍼 제거, Flat, extends BaseResponse
- [x] RecentEmailResponse — extends BaseResponse
- [x] CalendarEventResponse — @JsonProperty snake_case 추가
- [x] CalendarEventDetailResponse — @JsonProperty, extends BaseResponse
- [x] EmailListResponse — @JsonProperty snake_case 추가
- [x] EmailDetailResponse — @JsonProperty, extends BaseResponse
- [x] TemplateResponse — @JsonProperty, extends BaseResponse
- [x] SupportTicketListResponse — result_code/result_req 제거, extends BaseResponse
- [x] SupportTicketDetailResponse — result_code/result_req 제거, extends BaseResponse

### Phase 3: 신규 DTO 생성 ✅
- [x] CalendarEventListResponse, TemplateListResponse, BusinessResourceListResponse
- [x] FaqListResponse, CategoryListResponse, EmailPageResponse
- [x] InboxActionResponse, ProfileSaveResponse
- [x] ApiErrorResponse → BaseResponse로 통합

### Phase 4: Service 수정 ✅
- [x] AutomationService.getRules() — .success(true) 제거
- [x] NotificationService — success/message 제거, SimpleResponse → BaseResponse
- [x] DashboardService — 4개 메서드 플래튼화 빌더
- [x] InboxService.getInbox/getDetail — 플래튼화 빌더, reply/calendar → InboxActionResponse 반환
- [x] CalendarService.getEvents() — CalendarEventListResponse 반환
- [x] TemplateService.getTemplates() — TemplateListResponse 반환
- [x] BusinessService — List 래퍼 DTO 반환, upsertProfile → ProfileSaveResponse 반환
- [x] EmailService.getEmails() — EmailPageResponse 반환

### Phase 5: Controller 전체 수정 ✅
- [x] AuthController — 이미 정상
- [x] UserController — DeleteUserResponse/SuccessResponse → BaseResponse
- [x] IntegrationController — SuccessResponse → BaseResponse, request body 제거
- [x] AutomationController — 이미 정상
- [x] BusinessController — Map 응답 → ProfileSaveResponse, List → 래퍼 DTO, DELETE → BaseResponse
- [x] CalendarController — List → CalendarEventListResponse
- [x] DashboardController — 이미 정상
- [x] EmailController — Page → EmailPageResponse
- [x] InboxController — Map 응답 → InboxActionResponse
- [x] NotificationController — SimpleResponse → BaseResponse
- [x] SupportTicketController — 이미 정상
- [x] TemplateController — List → TemplateListResponse, DELETE → BaseResponse

### Phase 6: GlobalExceptionHandler 수정 ✅
- [x] ErrorResponse record → BaseResponse 형식으로 완전 교체

### Phase 7: 빌드 확인 ✅
- [x] mvn compile 성공 (178개 파일, 오류 없음)

## 최종 결과
- 전체 작업 완료: 2026-03-30
- 빌드: BUILD SUCCESS (178 source files)
- 경고: EmailAnalysisConsumer.java unchecked operations (기존 코드, 본 작업 범위 외)
- 모든 API 응답이 BaseResponse 공통 형식으로 통일됨

---

# RabbitMQ 큐 분리 및 메시징 파트 구현

## 목표
classify / draft 2-큐 구조로 분리하여 AI 파이프라인 연동 구축

## 체크리스트

### Phase 1: 큐 설정 분리 ✅
- [x] application.yml — queue.request/response → queue.classify/draft 교체
- [x] RabbitMQConfig.java — CLASSIFY_QUEUE, DRAFT_QUEUE 상수 + 빈 등록 (기존 request/response 제거)

### Phase 2: 엔티티 확장 ✅
- [x] EmailAnalysisResult.java — emailEmbedding TEXT 컬럼 추가, updateFromClassify() 메서드 추가
- [x] DraftReply.java — replyEmbedding TEXT 컬럼 추가, updateContent() 메서드 추가
- [x] DraftReplyRepository.java — findByEmail_EmailId() 추가

### Phase 3: Publisher 교체 ✅
- [x] EmailMessagePublisher.java — publishEmailAnalysisRequest() 제거
  → publishClassifyRequest() + publishDraftRequest() 2개로 분리
  → OutboxRepository 의존성 제거

### Phase 4: Consumer 구현 ✅
- [x] EmailAnalysisConsumer.java — 구 단일 컨슈머 스텁으로 대체 (@deprecated)
- [x] EmailClassifyConsumer.java — 신규 생성 (classify 결과 수신 → CalendarEvent 저장 → publishDraftRequest 연결)
- [x] EmailDraftConsumer.java — 신규 생성 (draft 결과 수신 → DraftReply 저장 → Email status 갱신)

### Phase 5: 연계 서비스 수정 ✅
- [x] EmailService.java — publishEmailAnalysisRequest() → publishClassifyRequest() 교체, Outbox 관련 코드 정리

### Phase 6: 빌드 확인 ✅
- [x] mvn compile 성공 (183개 파일, 오류 없음)

## 최종 결과
- 완료: 2026-04-02
- 빌드: BUILD SUCCESS (183 source files)
- embedding 필드: 2세션에서 VECTOR 타입으로 전환 예정 (현재 JSON TEXT로 저장)

---

# VECTOR 타입 매핑 및 유사도 추천 파트 구현

## 목표
embedding 필드를 MariaDB VECTOR(384) 바이너리로 전환하고, 코사인 유사도 기반 초안 추천 API 구현

## 체크리스트

### Phase 1: VectorConverter 구현 ✅
- [x] converter/VectorConverter.java — float[] ↔ byte[] (little-endian, 4bytes/float), @Component + @Converter

### Phase 2: 엔티티 전환 ✅
- [x] EmailAnalysisResult.java — emailEmbedding TEXT → float[] (@Convert VectorConverter), summary 필드 추가, updateFromClassify() 시그니처 변경
- [x] DraftReply.java — replyEmbedding TEXT → float[] (@Convert VectorConverter), updateContent() 시그니처 변경

### Phase 3: Consumer 수정 ✅
- [x] EmailClassifyConsumer.java — JSON 직렬화 제거, List<Number> → float[] 변환 후 직접 저장, ObjectMapper 의존성 제거
- [x] EmailDraftConsumer.java — JSON 직렬화 제거, List<Number> → float[] 변환 후 직접 저장, ObjectMapper 의존성 제거

### Phase 4: 추천 기능 구현 ✅
- [x] DraftReplyRepository.java — findTopKSimilarDrafts() 네이티브 쿼리 추가 (VEC_DISTANCE_COSINE)
- [x] dto/response/recommend/RecommendedDraftResponse.java — BaseResponse Flat 구조, DraftItem 내부 클래스
- [x] service/RecommendService.java — recommendSimilarDrafts(emailId, topK) 구현
- [x] controller/RecommendController.java — GET /api/emails/{emailId}/recommendations?topK=3

### Phase 5: 빌드 확인 ✅
- [x] mvn compile 성공 (오류 없음)

## 최종 결과
- 완료: 2026-04-02
- 빌드: BUILD SUCCESS
- embedding 저장 방식: JSON TEXT → MariaDB VECTOR(384) 바이너리 전환 완료

---

# RabbitMQ 파이프라인 전면 재설계 (RabbitMQ.md spec 기반)

## 목표
RabbitMQ.md spec에 따라 기존 임시 구현을 전면 교체:
- 큐/Exchange 이름 Terraform 관리 기준으로 정렬
- passive 선언 전환 (Spring이 RabbitMQ 리소스 생성 불가)
- rabbitMQ / sse 패키지 분리
- SKIP LOCKED 기반 분산 폴링
- SSE 연동 (@Profile("sse") 적용)
- Draft 파이프라인 제외

## 체크리스트

### Phase 0: 기존 파일 정리
- [ ] config/RabbitMQConfig.java — 삭제 (rabbitmq/config로 이전)
- [ ] controller/EmailController.java — 삭제 (git D 상태, 정리)
- [ ] messaging/* 구 파일들 — 삭제 (git D 상태, 정리)
- [ ] service/EmailService.java — 삭제 (git D 상태, 정리)

### Phase 1: 설정 파일 수정
- [ ] application.yml — exchange/queue 이름 변경 + sse pod URL 추가
- [ ] OutboxRepository.java — SKIP LOCKED 네이티브 쿼리 추가

### Phase 2: rabbitmq 패키지 신규 생성
- [ ] rabbitmq/config/RabbitMQConfig.java — passive 선언 + Publisher Confirms + RabbitAdmin
- [ ] rabbitmq/dto/OutboxPayloadDTO.java — Outbox → Queue 직렬화 DTO
- [ ] rabbitmq/dto/ClassifyResultDTO.java — AI 응답 역직렬화 DTO
- [ ] rabbitmq/publisher/MailPublisher.java — x.app2ai.direct 발행 + CorrelationData + ConfirmCallback
- [ ] rabbitmq/consumer/MailConsumer.java — q.2app.classify 수신, x-death count 기반 ack/nack
- [ ] rabbitmq/scheduler/MailScheduler.java — 10s 폴링 + 30min 타임아웃 롤백
- [ ] rabbitmq/service/MailService.java — 인터페이스
- [ ] rabbitmq/service/MailServiceImpl.java — 트랜잭션 관리 + 상태 전이 + SSE 브로드캐스트
- [ ] rabbitmq/controller/MailController.java — 관리자 Job 조회/삭제 API

### Phase 3: sse 패키지 신규 생성 (@Profile("sse"))
- [ ] sse/service/SseEmitterService.java — emitter 목록 관리
- [ ] sse/controller/SSEController.java — GET /api/mail/stream
- [ ] sse/controller/InternalSSEController.java — POST /internal/sse/push

### Phase 4: 빌드 확인
- [ ] mvn compile 성공

---

# 캘린더 스펙 확장 (calendar.md 기반)

## 목표
DB 수정사항 및 프론트 UI 스펙에 맞춰 캘린더 백엔드 확장

## 체크리스트
- [x] `CalendarEvent.java` — PK 컬럼명 event_id→calendar_event_id, eventType/location/description 필드 추가, update() 시그니처 확장
- [x] `CalendarEventRequest.java` — eventType/location/notes 필드 추가
- [x] `CalendarEventDetailResponse.java` — event_type/location/notes + email_sender_name/email_subject(flat) 추가
- [x] `CalendarEventResponse.java` — event_type/location 추가
- [x] `CalendarService.java` — createEvent/updateEvent에 새 필드 반영
- [x] mvn compile 성공 (오류 없음)

## 최종 결과
- 완료: 2026-04-06
- 빌드: BUILD SUCCESS
