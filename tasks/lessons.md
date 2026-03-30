# 교훈 기록 (lessons.md)

## 2026-03-30: BaseResponse 통일 작업

### L1. Lombok @Builder + extends BaseResponse 패턴
- **교훈**: `@Builder`가 붙은 하위 클래스에서 `extends BaseResponse`를 사용하면, Lombok이 생성하는 생성자가 암묵적으로 `super()`를 호출 → BaseResponse 기본 생성자(success=true, resultCode=200)가 자동 설정됨.
- **효과**: 빌더로 생성한 응답 객체에는 별도로 `.success(true)` 같은 필드를 지정할 필요 없음. 지정하면 오히려 "필드를 찾을 수 없음" 컴파일 오류 발생.
- **적용**: BaseResponse를 extends하는 모든 @Builder 클래스에서 success/result_code/result_req를 빌더에 넣지 말 것.

### L2. 서비스 메서드와 컨트롤러의 시그니처 불일치 버그
- **교훈**: 이전 세션에서 `GoogleOAuthService.deleteIntegration(Long userId, DeleteIntegrationRequest request)` → `deleteIntegration(Long userId)`로 변경했지만, `IntegrationController`에는 반영되지 않아 컴파일 오류 발생.
- **방지법**: 서비스 메서드 시그니처를 변경할 때, 해당 메서드를 호출하는 컨트롤러도 동시에 수정할 것. 한 번에 찾아서 같이 변경.

### L3. 삭제된 DTO를 import하는 컨트롤러 처리
- **교훈**: `SuccessResponse`, `DeleteUserResponse` 삭제 후 `UserController`가 여전히 import하고 있어 컴파일 오류 예상. 컨트롤러 수정을 서비스와 동시에 진행해야 함.
- **방지법**: DTO를 삭제할 때 반드시 `grep`으로 해당 클래스를 import/사용하는 파일 전체를 찾아서 함께 수정.

### L4. Windows 환경에서 mvn 컴파일 시 Java 버전 이슈
- **교훈**: bash 환경(WSL 경유 포함)의 `java` 명령어가 Java 17을 가리키지만, 프로젝트는 Java 21 필요. `mvn compile` 실패.
- **해결**: `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.4.7-hotspot"` 설정 후 `./mvnw.cmd compile` 실행.
- **적용**: 이 프로젝트에서 빌드 확인 시 반드시 Eclipse Adoptium JDK 21을 JAVA_HOME으로 지정해야 함.

### L5. Flat 구조 vs 중첩 data 객체
- **교훈**: CLAUDE.md의 "임의의 중첩 data 객체 금지" 규칙 = 단건 비즈니스 필드는 BaseResponse와 같은 레벨로 flat하게 노출. 다만 배열(`data: [...]`)은 허용.
- **적용**: `SummaryResponse.SummaryData`, `WeeklySummaryResponse.WeeklyData`, `InboxListResponse.InboxPage`, `InboxDetailResponse.DetailData` 같은 단순 래퍼 inner class는 제거하고 필드를 직접 부모 클래스로 올릴 것.

### L6. 리스트 응답 패턴
- **교훈**: 목록 API에서 `List<T>`를 직접 반환하면 BaseResponse 공통 필드가 없음.
- **패턴**: `XxxListResponse extends BaseResponse { List<XxxResponse> data; }` 구조로 래핑.
  - 목록 아이템 클래스(`XxxResponse`)는 BaseResponse를 상속하지 않음 (list item은 단독 응답 아님).
  - 단건 응답 클래스(`XxxDetailResponse`)는 BaseResponse를 상속.

### L7. GlobalExceptionHandler 통일
- **교훈**: 기존 `ErrorResponse` record가 BaseResponse 형식을 따르지 않아 에러 응답이 일관성 없었음.
- **해결**: 모든 ExceptionHandler를 `new BaseResponse(statusCode, message)` 패턴으로 통일. validation 오류는 필드별 메시지를 ","로 join하여 result_req에 담음.
