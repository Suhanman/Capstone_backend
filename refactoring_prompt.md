클로드, 기존 백엔드 설계에 대해 프론트엔드/인프라 팀원과 아주 중요한 아키텍처 변경을 합의했어. 기존의 네가 짠 구조(Job 생성 -> DB QUEUED -> 즉시 RabbitMQ Publish)에서 RabbitMQ Publish 단계를 걷어내고, 쿠버네티스(k8s) Job을 트리거하는 방식으로 변경하려고 해.

[아키텍처 변경 배경]
기존 구조처럼 Java 서버가 직접 DB에서 수만 건의 email/outbox 테이블을 JOIN해서 JSON으로 만들고 S3에 올리면 Java 서버(App 파드)에 심각한 부하(OOM 등)가 발생할 수 있어.
그래서 팀원이 **"데이터를 가공해서 S3에 올리고, AI 서버에 최종적으로 학습 트리거를 보내는 무거운 작업은 일회용 Go 언어 k8s Job 컨테이너가 담당"**하기로 했어.

[Java 백엔드의 변경된 역할 (매우 중요)]
이제 Java 백엔드는 RabbitMQ로 메시지를 직접 쏘지 않아. 대신 API 요청이 오면 DB(training_jobs)에 QUEUED 상태를 기록한 직후, Fabric8 Kubernetes Client (또는 공식 Java k8s 클라이언트)를 사용해서 미리 정의된 Go 언어의 k8s Job(일회성 파드)을 생성(Create/Apply)하는 역할만 담당해.
AI 서버가 학습을 끝내고 RabbitMQ(q.2app.training)로 완료 이벤트를 쏴주면, Java 서버가 그걸 수신해서 DB를 업데이트하는 컨슈머 역할은 기존 그대로 유지돼.

[구현 요청 사항]
기존에 작성해 준 AiTrainingServiceImpl을 이 새로운 아키텍처에 맞게 완전히 리팩토링해 줘.

1. Kubernetes API 연동 추가

Spring Boot에서 쿠버네티스 클러스터 내부의 Job 리소스를 생성할 수 있도록 KubernetesClient를 연동하는 설정(Config) 코드를 작성해 줘. (Fabric8 라이브러리 사용 권장)

2. k8s Job 트리거 로직 구현

createJobInternal() (또는 기존 작업 생성 메서드) 내부 로직을 변경해 줘.

기존: DB INSERT -> RabbitMQ Publish

변경: DB INSERT -> 쿠버네티스 API를 호출하여 Go 언어 Job 컨테이너 실행.

이때, Go Job이 자신이 어떤 작업을 해야 하는지 알 수 있도록 Job 파드를 띄울 때 **환경변수(Env)나 커맨드 아규먼트(Args)**로 job_id, job_type, dataset_version 등을 넘겨주도록 구성해 줘.

3. RabbitMQ Publisher 삭제

기존에 짰던 TrainingJobPublisher (요청 발행기)는 이제 쓰지 않으니 관련 코드는 걷어내 줘.

4. 기존 유지 항목

training_jobs, trained_models, training_datasets 3개 테이블로 나눈 구조와 Flat DTO 구조는 완벽하니까 100% 그대로 유지해.

TrainingResultConsumer (AI 완료 결과 수신부)도 그대로 유지해.

이 변경된 시나리오에 맞춰서, Spring Boot 환경에서 k8s Job을 띄우는 코드가 포함된 AiTrainingService 관련 파일들을 다시 작성해 줘!