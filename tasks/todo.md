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
- [x] BusinessResourceResponse — extends BaseResponse
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
