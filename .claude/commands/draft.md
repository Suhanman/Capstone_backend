# Draft (템플릿 관련 설계)


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

본 스펙은 템플릿 처리에 관해서 다룹니다.
## 최초 템플릿

### DraftPublihser.java
사용자는 회원가입 시 템플릿 생성을 위한 데이터를 BusinessProfile 테이블에 쓰기를 한다.
해당 쓰기 트랜잭션이 트리거로 쓰기가 일어난 로우를 RabbitTemplateconvertandSend(json변환)을 통해
x.app2ai.direct 에 2ai.draft를 Routing key 로 싣는다. 

### DraftConsumer.java
AI 에서 돌아온 답변이 q.2app.draft 에 실리면 그것을 구독하는 Consumer가 
ack 처리가 되었을 경우