# RabbitMQ 파이프라인 설계

## 조건
1. 본 spec은 자바 스프링 서버에서의 RabbitMQ 관련 코드를 처리하고 작성하기 위함이다.
2. 본 spec에 의해 작성되는 코드는 기술하는 내용만을 코딩해야하며 그외 비즈니스 로직에 일체 직접 수정/삭제/추가하는 행위가 일어나서는 안된다.
3. 본 spec은 solid를 최대한 지킨다.
4. 최대한 구현은 인터페이스에 의존한다.
5. RabbitMQ 파이프라인에 한해서 해당 spec은 가장 우선된다.
6. 해당 md에 의해서 생성되는 코드는 메시지 파이프라인은 rabbitMQ 폴더, SSE 관련은 sse 폴더에 별도 관리되어야한다.
7. 본 spec에서 Draft(Template) 리소스가 선언은 되어있지만 인지만 하고 실제 기능 생성에 관여하지 않는다. 별도 md로 추가 작성예정.

## 환경

[//]: # (1. 실제 RabbitMQ Exchange, Queue ,Routing key 등은 Terraform iac 에서 관리)

[//]: # (2. RabbitMQ 는 온프레미스 서버의 k8s 클러스터 내부에 존재)

[//]: # (3. AI 서버는 AWS ec2 내 존재 , wireguard VPN을 통한 통신)

1. 로컬호스트에서 테스트환경에서 우선 구축한다.
2. sse pod 등은 우선 환경변수처리
## 상세
1. 파이프라인은 크게 분류/템플릿 파일로 구분된다.



## 분류 파이프라인
1. 서비스 서버에서 구글 API 페이로드를 정재하여 DB 에 저장 (OutBox table)
2. Outbox 테이블을 폴링하는 스프링서버가 status 가 Ready인 email id의 데이터를 json 형태로 Queue에 실음
3. Outbox 테이블의 해당 emailId가 sending 으로 업데이트
4. AI 서버로 비동기식으로 전달 후 AI 역시 반환 큐에 실어서 서비스 서버로 반환
5. ack/nack 형식으로 RabbitMQ에서 DB로 저장 (Email_AI table) 및 Outbox status finished

## 설계원칙
1. 중복 책임 방지 및 클래스별 책임 명확화
2. 스프링부트에서는 RabbitMQ 리소스 생성할 권한이 없어야한다. 
3. DLX 설정, TTL 등 RabbitMQ의 리소스에 설정에 관한건 전부 terraform으로 관리되며 스프링부트에서는 일체 관여할수없다.
4. outbox 패턴은 outbox를 폴링하는 것만으로 메시지 발행이 가능해야한다.
## 주요 항목 및 문제해결
1. 분산 Pod 환경에서의 SSE 연결 문제
- SSE pod / App pod를 Spring Profile을 이용한 분리. SSE Pod는 Deployment 형식이아닌 stateful 방식으로 고정.
- Haproxy Stick Session을 통하여 세션 고정
- application.yaml에서의 명시적 sse pod 고정 dns 노출
  ① 브라우저가 SSE 연결 시도 → HAProxy Sticky Session → 항상 SSE Pod A로 고정
  ② SSE Pod A 메모리에 emitter{user123} 생성 (= 브라우저와 열린 HTTP 연결 객체)
  ③ APP Pod가 Outbox 폴링 → RabbitMQ q.ai.inbound에 메일 publish
  ④ AI Server가 VPN으로 수신 → 분류/요약/답장 생성 → q.ai.result에 결과 publish
  ⑤ APP Pod가 q.ai.result consume → DB 저장 (markFinished)
  ⑥ APP Pod → SSE Pod A, B 전부에 POST /internal/sse/push (브로드캐스트)
  ⑦ SSE Pod A: emitter 있음 → emitter.send() → 브라우저에 실시간 알림
  SSE Pod B: emitter 없음 → notifyIfPresent() 조용히 무시
  ✓ emitter = 브라우저↔SSE Pod 사이의 열린 HTTP 연결 객체. Pod 메모리에만 존재.
  ✓ APP Pod / SSE Pod 완전 분리 → SSE Pod는 emitter 관리만, APP Pod는 비즈니스 로직만.
  ✓ WireGuard VPN = 온프레미스 K8s ↔ AWS EC2 간 암호화 통신 터널.,
2. 분산 Pod 환경에서의 메시지 중복 및 폴링 중복 문제
- Skipped Lock을 통한 중복 폴링 방지
- 트랜잭션 내에서 status 변경
- 초기화 시점에 리소스 체크 로직
3. Retry 정책 설정
- 메일 전송 큐 - Retry 큐 서로가 서로를 DLX로 지정.
- 최대 3회 반복후 실패시 쓰레기통 큐로 이동
4. VPN 장애 대응 (하이브리드 클라우드 환경)
- heartbeat 패턴을 통한 AI 서버측에서 주기적 VPN 연결체크
5. 각 단계별 파이프라인 병목 감지시 대처
5-1. 서비스 서버 -> RabbitMQ 처리 실패
- 서비스 서버에서 메시지 브로커로 전송 자체가 실패했다면 Outbox에서 status만 sending으로 되어있고 그 어떤 것도 반환되지 않는다. sending status가 30분 이상 지속되면 Ready로 되돌린다.
5-2. RabbitMQ -> AI 서버 처리 실패
- Retry를 이용해 최대 3번 재시도 후 실패하면 해당 큐를 쓰레기통 큐로 이동 후 Failed 처리
5-3. AI 서버 -> RabbitMQ 처리 실패
- AI 서버는 DB가 없으므로 처리 결과를 영속화할 수 없다.
- 연결 복구 후 재시도를 시도하나, 연결 자체가 소실된 경우
  원본 메시지(q.ai.inbound)의 ack가 전송되지 않아 RabbitMQ가 재전달한다.
- AI 서버는 동일 메일을 재처리하게 되며, 이는 LLM 중복 호출을 유발한다.
- 이를 허용하는 대신, App 서버의 markFinished()에서
  emailId 기준 FINISHED 상태를 사전 확인하여 중복 저장을 방지한다. (멱등성 보장)
- 결과적으로 데이터 정합성은 보장되나, 장애 빈도에 비례한 LLM 비용이 발생할 수 있다
5-4. RabbitMQ -> 서비스 서버 처리 실패
- Retry를 이용해 최대 3번 재시도 후 실패하면 해당 큐를 쓰레기통 큐로 이동한다.

### 서비스 서버
2. k8s Ha 에 의하여 워커 늘어날 가능성 존재 / 메시지 중복 방지 처리 로직 필요
3. Outbox 폴링 중복 문제 해결 필요
4. 멀티 파드 환경에서의 sse 연결 문제 (RabbitMQ 포함) => SSE 프로필 / APP 프로필 완전 구분한 배포로 해결.
5. profile 전부 app 적용 단 , sse 로 명시된 리소스만 sse 적용
### AI 서버
4. AI 처리 실패
5. Publish 실패 : VPN 에러 / RabbitMQ 연결 끊김 / 네트워크 지연
6. AI 측에서는 결과를 json 으로 반환.
7. AI 측 DB 없음.


## RabbitMQ 리소스 설정 (Terraform을 통한 관리. spec.md에서는 명시만 함.)
### Exchange
#### x.app2ai.direct
Type : direct
durable : true 

#### x.ai2app.direct
Type : direct
durable : true

#### x.retry.direct
Type : direct
durable : true


### Queue
#### q.2ai.classify
Publisher : App Server
Consumer : AI Server
Binding Key : 2ai.classify
DLX : x.retry.direct
x-dead-letter-routing-key: 2ai.classify.retry

#### q.2ai.draft
Publisher : APP Server
Consumer : AI Server
Binding Key : 2ai.draft
DLX : x.retry.direct
x-dead-letter-routing-key: 2ai.draft.retry

#### q.2app.classify
Publisher : AI Server
Consumer : APP Server
binding key: 2app.classify
DLX: x.retry.direct
x-dead-letter-routing-key: 2app.classify.retry

#### q.2app.draft
Publisher : AI Server
Consumer : APP Server
binding key: 2app.draft
DLX: x.retry.direct
x-dead-letter-routing-key: 2app.draft.retry


#### q.2ai.classify.retry
durable: true
x-message-ttl: 30000
x-dead-letter-exchange: x.app2ai.direct
x-dead-letter-routing-key: 2ai.classify
binding key : 2ai.classify.retry

#### q.2ai.draft.retry
durable: true
x-message-ttl: 30000
x-dead-letter-exchange: x.app2ai.direct
x-dead-letter-routing-key: 2ai.draft
binding key: 2ai.draft.retry

#### q.2app.classify.retry
durable: true
x-message-ttl: 30000
x-dead-letter-exchange: x.ai2app.direct
x-dead-letter-routing-key: 2app.classify
binding key : 2app.classify.retry

#### q.2app.draft.retry
durable: true
x-message-ttl: 30000
x-dead-letter-exchange: x.ai2app.direct
x-dead-letter-routing-key: 2app.draft
binding key : 2app.draft.retry



#### q.dlx.failed
3번의 재시도가 실패한 메시지가 publish 로 모임
Exchange와 routingkey 필요없는 재시도 최종 실패한 메시지가 모이는 쓰레기통
default exchange를 사용해서 queue 이름을 routing key로 직접 publish

## 코드 작성 (Onprem java spring boot)
### CapstoneProject/src/main/java/RabbitMQ
#### RabbitMQConfig.java
- Terraform에서 생성한 Exchange / Queues/ RoutingKey 가져와 passive 선언
- Jackson2JsonMessageConverter 객체 생성 
- RabbitTemplate 인스턴스 생성후 bean 저장
- RabbitTemplate의 Publisher Confirms/Returns 콜백 설정.
- RabbitAdmin Bean 등록을 통한 초기화 시점에 리소스 체크 로직
- DLQ 안의 메시지 조회

#### MailPublisher.java
(스케쥴러에서 넘겨받아 서비스 서버 -> RabbitMQ로 메일을 보내는 역할)
- RabbitTemplate final 로 호출 후 생성 convertandsend(json)
- publish 인스턴스 생성 후 json 전환 후 x.app2ai.direct 로 실어 보냄
- CorrelationData에 emailid 적재 + (메시지에 같이 CorrelationData 실음)
- ConfirmCallback에서 ack=false(발송 실패)가 오면, MailService.markFailed(id)를 호출

#### MailConsumer.java
(AI서버로 부터 RabbitMQ를 거쳐서 온 메일을 관리하고 SSE 파드에 방아쇠 역할)
  - q.ai.result 리스너 설정
  - Prefetch Count = 1
  - x-death 헤더에서 count 읽어서(x-death[0].count) ack/nack 반환
  - 브로드캐스트 방식으로 mailService.markFinished()의 완료커밋 발행후 완료된 SSE pod 에 emitter 넣기
 
***
처리 성공 흐름
수신 → 처리 → markFinished() [트랜잭션 커밋]
→ AFTER_COMMIT 이벤트 발화 → SSE 브로드캐스트 → basicAck

처리 실패 흐름
수신 -> 어떤이유로 처리에 실패 -> nack 발행 -> retry count 1 증가 후 retry 큐에 실림 -> 다시 반복 
-> 3회 이상 실패시 쓰레기통 큐로 이동 후 status = Failed.
***

#### MailScheduler.java
(RabbitMQ로 Status = Ready인 메일을 감지하기 위한 스케쥴러)
- 10초 주기로 Outbox 테이블 폴링 Status = ready 인 항목.
- List readyList 실어서 저장 후 forEach문으로 mailPublisher 객체의 publish 메서드에 하나씩 넣어서 호출
- 30 분이상 Sending 상태인 Outbox 내의 이메일 id는 Ready로 되돌린다.

#### MailController.java
(외부 HTTP 내보내기 용)
- 프론트 대시보드 현황용
GET /api/admin/operations/jobs
GET /api/admin/operations/jobs/summary
GET /api/admin/operations/jobs/{job_id}
DELETE /api/admin/operations/jobs/{job_id}
DLQ 메시지 Get 조회 가능
 
#### MailService.java
(책임: 트랜잭션 관리 + 상태 업데이트 + 각종 모니터링을 위한 JPA)
- 트랜잭션안에서 발송된 메시지 status sending 업데이트 및 롤백
- MailConsumer ack/nack 기반 3회 미만 retry일시 nack / 3회 이상이면 status = failed 처리후 q.dlx.failed push
- MailConsuemr ack/nack 후 Outbox status = FINISHED 업데이트 / Failed 처리 
- Worker 복제시 중복 폴링 방지 : SKIP LOCKED 설정
- forceDelete() → 관리자 강제 삭제
- getJobList() → 목록 조회
- getJobSummary() → 상태별 건수 조회
- getJobDetail() → 상세 조회 (Outbox + Email_AI 조인)
- markPubilshedFailed()는 ack=false 시 READY(RabbitMQ로 app pod에서 메시지를 보내지 못했을때)
- markFailed()는 Outbox SENDING → FAILED 확정
- markFinished()  DB 저장 및 상태 변경 후 @TransactionalEventListener(AFTER_COMMIT) 으로 SSE 브로드캐스트 발화
- 처리 시작 시점 전에 finished 확인

#### MailServiceImpl.java
- MailService 인터페이스 구현체

#### OutboxEntity.java
- Outbox 테이블 매핑

#### OutboxDTO.java
- Outbox json으로 직렬화

#### ClassifyResultEntity.java
- Email_AI 테이블 매핑

#### ClassifyResultDTO.java
- AI 답변 직렬화 

#### InternalSSEController.java
- Profile("sse") 명시
- APP Pod에서 오는 HTTP 수신
- POST /internal/sse/push
- APP Pod에서 AI 결과 수신 → SseEmitterService로 위임
#### SseEmitterService.java
- Profile("sse") 명시
- emitter 목록 관리
- createEmitter() → emitter 생성 및 목록 등록
-  notifyIfPresent() → emitter 있을 때만 push
#### SSEController.java
- Profile("sse") 명시
- 프론트 SSE 연결 수립
- GET /api/mail/stream
- SSE 연결 수립 및 SseEmitter 반환
#### application.yaml
- sse pod는 deployment 형태가 아닌 StatefulSet로 배포하여 k8s dns 등록, sse pod 2개를 명확히 등록해서 app pod에서 sse pod를 강제적으로 알수있게함.
## 코드 작성 (AWS AI 서버 python)

#### settings.py (=application.yaml)

#### connection.py
- prefetch count =1 
- RabbitMQ 리소스 passive 선언 (생성권한 없음)
- 별도 스레드로 heartbeat 관리


#### mail_publisher.py
- x.ai2app.direct 발행 및 AI 답변 json q.ai.result 실어서 보냄
     

#### mail_consumer.py
- q.ai.inbound 구독 
- x-death 헤더에서 count 읽어서 retry_count 3 미만 nack 하여 재시도 3 q.dlx.failed publish 후 ack 처리
- Publish Confirms ack 순서 제어보장 (publish 후 브로커에게서 confirm을 대기하고 성공시 ack 실패시 nack retry 처리 3회 시도 후 쓰레기 큐 이동)

#### main.py
- Docker 진입점 및 모델 -> RabbitMQ 연결 - 메시징 서버 순의 순차적 백그라운드 실행

