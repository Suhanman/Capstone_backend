서비스 서버에서 SSE 겸업 -> 별도의 SSE hub 로 옮겨감.
따라서 본 프로젝트에서 아래와 같은 작업을 순차적으로 수행함.

1. 안전하게 SSE 관련 리소스 제거. 만약 다른 도메인의 파일 건드려야할 시 반드시 yes 후 진행.
2. SSE Hub와의 이벤트 통신을 RabbitMQ를 활용해서 처리함.
3. x.see.fanout 익스체인지가 이미 생성되어있고 config에서 다른 리소스처럼 가져오기만 할것.
4. fanout이기 때문에 별도의 routing key 필요없음.
5. sse emitter를 체크하기 위한 fanout 익스체인지로 발송될 json은 user_id와 구분을 위한 하나의 metadata 면충분함.
6. 이벤트의 영속성은 보장하지않아도 되기때문에 별도의 Entitiy는 필요없다.
7. Gmail pub/sub의 경우 emails table의 wirte transaction 이 finish 된 후 완료한 email_id의 user_id를 select, sse-type : pub/sub 이라는 데이터를 추가하여 json화.
8. 그렇게 json이 완성되면 RabbitMQ의 x.sse.fanout 으로 publish 하는 것으로 서비스 서버의 역할은 끝난다.

해당 spec 에서는 예시로 든 Gmail pub/sub의 케이스를 구현하는 것을 목표로한다.
plan 하는 와중 Solid 적인 부분은 Claude가 arch한 후, developer의 검토를 받는다.
해당 spec을 읽은 후 반드시 plan 수립하고 허락이 떨어지면 작업에 들어간다.