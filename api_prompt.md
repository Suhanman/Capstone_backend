[시스템 배경지식 주입]
현재 우리는 이메일 AI 에이전트 서비스를 개발 중이며, 백엔드와 AI 서버는 RabbitMQ를 통해 비동기 통신을 하고 있어. 전체적인 파이프라인 흐름은 다음과 같아.

1. 구글에서 새 메일이 오면 Google Pub/Sub이 우리 백엔드(Webhook API)로 알림을 보냄.

2. 백엔드(Webhook)는 이 알림을 받아 구글 API로 실제 이메일 본문을 조회함.

3. 조회된 데이터를 DB의 Outbox 테이블에 저장함 (Status: READY).

4. MailScheduler가 Outbox를 폴링하여 RabbitMQ의 x.app2ai.direct 익스체인지(q.ai.inbound 큐)로 메일 데이터를 Publish 함.

5. AI 서버가 이를 Consume 하여 분석/답장을 생성하고 다시 RabbitMQ로 돌려보냄.

[당면 과제]
위 파이프라인의 가장 첫 단계인 **"Google Pub/Sub Push 알림 수신 Webhook API (POST /api/webhook/pubsub)"**를 개발해야 해.

claude.md의 'Plan First' 원칙에 따라, 코드를 바로 짜지 말고 이 Webhook API의 상세 스펙(Spec) 설계안을 먼저 제안해 줘. >
[설계 시 반드시 지켜야 할 요구사항]

1. Method & URI: POST /api/webhook/pubsub

2. Request 스펙: >    - Google Pub/Sub이 보내는 표준 Push JSON payload 형식을 정확히 반영할 것. (message.data가 Base64 인코딩되어 들어오는 구조)

3. Response 스펙 (매우 중요 🚨):

구글 Pub/Sub은 200/201/204/102 등의 성공 HTTP Status 코드를 받지 못하면 메시지를 재전송함.

따라서 응답은 반드시 HTTP 200 OK를 반환해야 함.

하지만 우리 프로젝트의 엄격한 규칙인 BaseResponse 공통 응답 형식을 절대 무시해선 안 됨. (즉, 구글이 원하는 200 OK 상태 코드와 함께, 우리 시스템 규격인 success: true, result_code: 200 등이 포함된 JSON을 반환하도록 설계할 것)

4. 보안 검증 흐름 기획:

구글에서 온 정상적인 요청인지 확인하기 위한 서명 검증(JWT / Token) 방식을 어떻게 할 것인지 기획안에 포함할 것. (예: 쿼리 파라미터 토큰 방식 or 헤더 검증 방식)

5. 내부 파이프라인 연결 기획:

Webhook API가 데이터를 받으면 즉시 DB나 RabbitMQ 로직을 타야 할까, 아니면 비동기 이벤트로 넘겨야 할까? 빠른 200 OK 응답을 위해 이 Webhook Controller가 내부적으로 이메일 처리 서비스(추후 만들 로직)를 어떻게 호출해야 할지 아키텍처 관점의 제안을 포함할 것.

설계안을 마크다운 표와 설명으로 깔끔하게 정리해 주면, 내가 검토하고 승인한 뒤에 실제 코드 작성 지시를 내릴게.