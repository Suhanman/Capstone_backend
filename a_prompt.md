이전에 승인한 설계안을 바탕으로, 실제 비동기 이메일 본문 수집 및 DB 저장 로직과 Gmail watch() 구독 호출 로직을 구현해 줘.

1. PubSubHandlerService.handleAsync() 핵심 데이터 파이프라인 로직:

조회된 사용자의 토큰으로 Gmail API를 호출해 신규 메일의 본문(Plain text)을 가져와.

[중요] 데이터 저장 순서 (반드시 @Transactional 적용):
① 먼저 Emails 엔티티에 메일 데이터를 저장해 (email_id 획득).
② 발급된 email_id와 제목, 본문, 발신자 등을 조합해 AI 서버로 보낼 JSON 문자열(payload)을 생성해. (ObjectMapper 활용)
③ Outbox 엔티티를 생성하고, status는 'READY', payload에는 방금 만든 JSON 문자열을 넣어 DB에 저장해.

비동기 스레드이므로 실패 시 전역 예외 처리를 타지 않으니, 내부 전체를 try-catch로 감싸고 log.error를 남겨줘.

2. GoogleOAuthService.handleCallback() 내 watch() 추가:

토큰 교환 및 Integration DB 저장이 완료된 직후, 해당 사용자의 Gmail에 대해 watch()를 호출해 줘.

WatchRequest 생성 시 사용할 topicName은 환경변수(app.google.topic-name)에서 주입받도록 해.

호출 결과로 반환되는 historyId와 만료 시간(expiration)은 info 로그로 기록해 줘.

3. 엔티티 및 스펙 준수:

현재 Outbox 테이블은 email_id를 FK로 가지고 있고, payload 컬럼은 JSON 타입이야.

내가 담당하는 부분은 RabbitMQ로 보내기 전 Outbox 테이블에 쌓아두는 것까지이므로, 퍼블리싱이나 스케줄링 코드는 작성하지 마.

모든 API 응답은 기존에 약속한 BaseResponse 형식을 엄격히 따를 것.

DB 쿼리는 db.md 내용을 참고할 것. 
