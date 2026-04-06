# 프로젝트 개요
- 프로젝트명: LLM 기반 업무용 이메일 AI 에이전트
- 목적: 사용자의 이메일을 수집/분석하여 LLM 기반으로 업무를 자동화하는 AI 에이전트 서비스

# 기술 스택
- Backend: Java 21, Spring Boot 3.5.12, Maven
- Database: MariaDB
- ORM: Spring Data JPA 기반
- Message Queue: RabbitMQ (이메일 수집 및 LLM 비동기 처리용)
- Frontend: React

# 담당 범위
이 파일은 백엔드 팀 전체 공통 규칙입니다.
담당자별 역할은 하단 '담당자별 역할 분리' 섹션을 참고하세요.

# 핵심 아키텍처 및 인증 룰 (매우 중요)
1. 투트랙 인증 체계:
    - 서비스 자체 인증: `Authorization: Bearer <JWT>` 헤더 사용
    - 외부 API 연동: Google OAuth 2.0 (Access/Refresh Token을 DB `Integrations` 테이블에 저장)

2. 서비스 자체 인증 룰:
    - 현재 서비스 인증은 JWT 기반으로 동작한다.
    - JWT에서 추출한 사용자 식별자(userId) 기준으로 로그인 사용자를 판별한다.
    - `/me` 계열 API는 항상 로그인한 본인 기준으로 구현한다.

3. Google OAuth 연동 룰:
    - 상태 유지(State)는 서버 세션이나 DB 대신, 짧은 수명의 Stateless 임시 JWT를 생성하여 `state` 파라미터로 사용한다.
    - Google API 호출 시 순수 HTTP 요청이 아닌 공식 Java 클라이언트 라이브러리(`google-api-client`, `google-api-services-gmail` 등)를 사용한다.
    - 세밀한 권한 제어(Granular Consent)를 고려해, 토큰 발급 후 필수 Scope(Gmail, Calendar) 누락 여부를 반드시 검증한다.
    - Scope, callback 처리 방식, 상태 변경(status) 의미가 명확하지 않으면 임의로 구현하지 말고 먼저 사용자에게 질문한다.

## API 구현 원칙
1. API Method / URI / 요청·응답 필드명 / 인증 방식은 이미 확정된 API 설계서를 절대 기준으로 한다.
2. 임의로 URI를 변경하거나 응답 JSON 구조를 바꾸지 않는다. **(모든 응답은 하단에 정의된 '공통 응답 형식'을 최우선으로 따른다.)**
3. 없는 스펙은 추측하지 말고 사용자에게 먼저 질문한다.
4. Controller 매핑만 있고 내부 로직이 비어 있으면 구현 완료로 간주하지 않는다.

# 백엔드 코딩 컨벤션
1. 계층 분리:
   - Controller, Service, Repository, DTO 역할을 엄격히 분리한다.
   - 기본 패키지 구조(`controller`, `service`, `repository`, `domain/entity`, `dto/request`, `dto/response`, `security`, `exception`)를 우선 유지한다.

2. DB 접근:
   - 기본 DB 접근 방식은 Spring Data JPA를 사용한다.
   - 특별한 이유 없이 JdbcTemplate/MyBatis를 임의로 도입하지 않는다.

3. 공통 응답 형식 (매우 중요 🚨):
   - 모든 API 응답(성공 및 예외 에러 모두)은 반드시 `BaseResponse`를 상속받아 구현한다.
   - 모든 응답의 최상위 JSON 구조는 다음 4가지 공통 필드를 필수로 포함해야 한다.(첨부파일 다운로드 api는 예외)
      1) `content_type`: "application/json" (고정)
      2) `success`: boolean (성공 시 true, 에러 시 false)
      3) `result_code`: int (HTTP 상태 코드 사용. 예: 200, 400, 403)
      4) `result_req`: String (성공 시 빈 문자열 "", 에러 시 구체적 사유 및 내부 에러 코드 명시)
   - 개별 API의 비즈니스 데이터는 위 공통 필드와 같은 레벨(Flat 구조)에 추가한다. 임의의 중첩된 `data` 객체를 새로 만들지 않는다.
   - 하위 응답 클래스에서 `success`, `content_type` 등을 중복 선언하여 부모의 필드를 덮어쓰지 않도록 주의한다.

4. 예외 처리:
   - `try-catch`로 뭉뚱그리지 않고, `@RestControllerAdvice`를 활용한 전역 예외 처리(`GlobalExceptionHandler`)를 지향한다.
   - 전역 예외 처리기에서 반환하는 에러 객체(`ApiErrorResponse` 등) 역시 반드시 위의 '공통 응답 형식'을 완벽히 준수하여 반환해야 한다.

5. 주석:
   - 비즈니스 로직이 복잡한 부분(예: JWT 파싱, OAuth 검증 로직)은 한국어로 동작 과정을 상세히 주석으로 단다.

6. 명명 규칙 (Naming Convention):
   - JSON 응답 규격 등 외부로 노출되는 API 필드명은 소문자 기반의 `snake_case`를 엄격히 적용한다.
   - Java 내부 클래스, 메서드, 변수명은 자바 표준 관례인 `camelCase`를 사용하되, JSON 매핑 시 `@JsonProperty` 등을 활용하여 규격을 맞춘다.

7. Lombok:
   - Lombok annotation processing 이슈 가능성을 고려한다.
   - 생성자 주입이 중요한 클래스는 필요 시 명시적 생성자를 우선 제안한다.

# 담당자별 역할 분리

## 서비스 CRUD 담당 (비즈니스 설정 / 대시보드 / 수신함 / 템플릿 / 캘린더 / 자동화 / 알림)
- 담당 API: /api/business/**, /api/dashboard/**, /api/inbox/**, /api/templates/**, /api/calendar/**, /api/automations/**, /api/notifications/**
- Google OAuth / Gmail / RabbitMQ 관련 코드는 수정하지 않는다.
- 핵심 규칙:
    - @CurrentUser로 userId 주입
    - @Transactional(readOnly = true) 읽기 전용에 적용
    - N+1 방지: 연관 엔티티는 FETCH JOIN 사용
    - 본인 소유 데이터인지 userId 검증 필수

## Google OAuth / Gmail 연동 / 관리자 담당
- 담당 API: /api/integrations/**, /api/auth/**, /api/admin/**
- 위 핵심 아키텍처 및 인증 룰 참고

## 메시지 파이프라인 연동 담당
- 담당 md : RabbitMQ.md , draft.md, infra.md
- 1번 : 서비스 CRUD 및 Google OAuth, Gmail 연동 등의 비즈니스 로직에 일체 간섭하지 않는다.
- 2번 : 부득이하게 2번을 어겨야 할경우 반드시 검토받는다.


# 워크플로우 원칙 

## 1. 플랜 모드 기본화
- 3단계 이상의 작업이나 구조적 결정이 필요한 경우 반드시 계획부터 세운다.
- 계획은 tasks/todo.md에 체크 가능한 항목으로 작성한다.
- 구현 시작 전 반드시 계획을 사용자에게 확인받는다.
- 문제가 생기면 즉시 멈추고 재계획한다.

## 2. 서브에이전트 활용
- 조사, 탐색, 병렬 분석은 서브에이전트에 위임해 메인 컨텍스트를 깔끔하게 유지한다.
- 서브에이전트는 태스크 하나씩 집중 실행한다.

## 3. 자가 개선 루프
- 사용자가 실수를 수정해 주면 tasks/lessons.md에 패턴을 기록한다.
- 같은 실수를 방지하기 위한 규칙을 스스로 업데이트한다.
- 세션 시작 시 lessons.md를 읽고 반영한다.

## 4. 완료 전 검증 필수
- 빌드 성공 확인 없이 작업 완료로 간주하지 않는다.
- 변경 전후 동작 차이를 확인한다.
- "시니어 개발자가 승인할 수준인가?" 스스로 검토 후 제출한다.

## 5. 우아함 추구
- 비자명한 변경에는 "더 세련된 방법이 없을까?" 스스로 질문한다.
- 땜질식 수정 금지. 근본 원인을 해결한다.
- 단순하고 명백한 버그 수정에는 과도한 고민 생략.

## 6. 자율적 버그 수정
- 버그 리포트를 받으면 로그와 에러를 스스로 확인하고 수정한다.
- 하나하나 지시를 기다리지 않는다.
- CI 테스트 실패 시 말하지 않아도 스스로 수정한다.

# 태스크 관리 원칙
1. Plan First: 작업 전 tasks/todo.md에 체크리스트 작성
2. Verify Plan: 구현 전 계획 확인
3. Track Progress: 완료 항목은 즉시 체크 표시
4. Explain Changes: 각 단계마다 변경사항 한국어로 요약 보고
5. Document Results: todo.md에 최종 결과 리뷰 섹션 추가
6. Capture Lessons: 수정 발생 시 tasks/lessons.md에 교훈 기록

# 핵심 원칙
- Simplicity First: 모든 변경은 최대한 단순하게. 최소한의 코드만 건드린다.
- No Laziness: 임시방편 금지. 근본 원인을 찾아 시니어 개발자 수준으로 해결한다.
- Minimal Impact: 필요한 부분만 수정. 새로운 버그 유발 금지.

# AI 도우미(Claude) 행동 지침
- 코드를 제공할 때는 파일 경로와 패키지명(`com.emailagent...`)을 명확히 명시한다.
- 불필요하거나 쓰이지 않는 import, 더미 코드는 최대한 제외한다.
- Java 21, Spring Boot 3.5.12와 호환되지 않는 레거시 라이브러리는 사용하지 않는다.
- 모호한 부분이 있다면 임의로 가정을 세워 코드를 짜지 말고, 반드시 먼저 질문한다.
- 기존 코드를 수정할 때는 수정 대상 파일 목록을 먼저 제시하고, 그 다음 코드를 제안한다.