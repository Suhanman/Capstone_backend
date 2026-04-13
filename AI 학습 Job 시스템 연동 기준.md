## AI 학습 Job 시스템 연동 기준 문서

### Backend 연동 구현용 최종 문서

---

## 1. 문서 목적

본 문서는 현재 AI 서버 측에서 구현 및 검증이 완료된 **AI 학습 Job 시스템**을 기준으로,
**Backend가 추가로 구현해야 할 연동 범위와 기술 스펙**을 정리한 문서이다.

이 문서의 목적은 다음과 같다.

* Backend가 AI 학습 Job 시스템과 연동하기 위한 전체 구조를 이해할 수 있도록 한다.
* RabbitMQ 요청/완료 이벤트 스펙을 명확히 전달한다.
* Backend가 구현해야 할 REST API, MQ producer/consumer, DB 반영 로직을 구체화한다.
* 현재 완료된 범위와 미구현 범위를 분리하여 작업 범위를 명확히 한다.

---

## 2. 전체 구조 개요

현재 학습 Job 시스템은 다음 구조를 기준으로 동작한다.

```text
[관리자 웹]
    ↓
[Backend REST API]
    ↓
Job 생성
    ↓
DB 저장 (training_jobs, status = QUEUED)
    ↓
RabbitMQ 요청 발행
    ↓
q.2ai.training
    ↓
[AI Training Worker]
    ↓
학습 작업 수행
    ↓
완료 이벤트 발행
    ↓
q.2app.training
    ↓
[Backend Result Consumer]
    ↓
training_jobs 상태 업데이트
    ↓
[Backend REST API]
    ↓
관리자 웹 상태 조회
```

---

## 3. 구조 흐름도

### 3.1 Job 생성부터 완료까지 전체 흐름

```text
1. 관리자 웹에서 재학습 요청
2. Backend가 Job 생성 API 처리
3. Backend가 training_jobs 테이블에 Job 저장
4. Backend가 q.2ai.training 으로 요청 메시지 발행
5. AI training worker가 메시지 consume
6. AI worker가 학습 수행
7. AI worker가 q.2app.training 으로 완료 이벤트 발행
8. Backend consumer가 완료 이벤트 consume
9. Backend가 training_jobs 상태 및 결과 업데이트
10. 관리자 웹은 GET API로 상태 조회
```

---

## 4. 현재까지 완료된 범위

### 4.1 AI 서버 측 완료된 항목

현재 AI 서버 측에서는 아래 항목이 구현 및 검증 완료되었다.

| 구분                      | 완료 여부 | 설명                              |
| ----------------------- | ----- | ------------------------------- |
| Training 요청 스키마         | 완료    | `q.2ai.training` 요청 메시지 스키마 구현  |
| Training 완료 이벤트 스키마     | 완료    | `q.2app.training` 완료 메시지 스키마 구현 |
| Training worker         | 완료    | `consumer_training.py` 구현 완료    |
| Training 요청 consume     | 완료    | `q.2ai.training` consume 확인 완료  |
| Training safe mode      | 완료    | 기본 safe mode로 worker 테스트 가능     |
| Training actual mode 연결 | 완료    | 기존 학습 함수 연결 완료                  |
| 완료 이벤트 publish          | 완료    | `q.2app.training` publish 확인 완료 |
| Ack 처리                  | 완료    | consume 후 ack 정상 처리 확인          |
| RabbitMQ E2E 테스트        | 완료    | 요청 큐 → worker → 결과 큐 흐름 검증 완료   |

### 4.2 Backend 측 미구현 항목

현재 Backend는 아래 항목이 아직 구현되지 않은 상태다.

| 구분                  | 상태              | 설명                           |
| ------------------- | --------------- | ---------------------------- |
| Job 생성 API          | 미구현             | 재학습 요청 REST API 필요           |
| training_jobs DB 저장 | 미구현             | Job 생성 시 DB insert 필요        |
| 요청 MQ 발행기           | 미구현             | `q.2ai.training` publish 필요  |
| 결과 MQ consumer      | 미구현             | `q.2app.training` consume 필요 |
| Job 상태 업데이트         | 미구현             | 완료 이벤트 수신 후 DB update 필요     |
| Job 상태 조회 API       | 미구현             | `GET /jobs/{job_id}` 필요      |
| 모델 관리 API           | 미구현 또는 별도 확인 필요 | 모델 버전 조회/활성화 필요              |

---

## 5. Backend가 구현해야 할 핵심 항목

Backend는 아래 4개 축을 중심으로 구현하면 된다.

### 5.1 REST API 계층

Backend는 관리자 웹과 직접 연결되는 API 계층을 구현해야 한다.

필요 API:

* Job 생성 API
* Job 상태 조회 API
* 모델 관리 API

### 5.2 Job DB 관리 계층

Backend는 `training_jobs` 테이블을 기준으로 비동기 Job 상태를 관리해야 한다.

필요 기능:

* Job 생성 시 insert
* 완료 이벤트 수신 시 update
* 상태 조회 시 select

### 5.3 RabbitMQ 요청 발행기

Backend는 Job 생성 후 AI worker가 처리할 수 있도록 `q.2ai.training` 큐로 메시지를 발행해야 한다.

### 5.4 RabbitMQ 완료 이벤트 consumer

Backend는 AI worker가 발행한 완료 이벤트를 `q.2app.training` 에서 consume하고 DB를 업데이트해야 한다.

---

## 6. Backend가 구현해야 할 REST API

### 6.1 데이터셋 관리 API

| Method | URI                                    | 설명                           |
| ------ |----------------------------------------| ---------------------------- |
| GET    | `/api/admin/ai-training/datasets`      | 등록된 데이터셋 버전, 건수, 상태 목록 조회    |
| POST   | `/api/admin/ai-training/dataset-collections` | 원천 데이터 기반 학습용 데이터 재수집 Job 생성 |

### 6.2 전처리 및 학습 작업 API

| Method | URI                                        | 설명                                |
| ------ |--------------------------------------------| --------------------------------- |
| POST   | `/api/admin/ai-training/preprocessing-jobs` | 결측 제거, 공백 정리, `email_text` 생성 작업  |
| POST   | `/api/admin/ai-training/pair-jobs`               | SBERT 파인튜닝용 pair 생성 작업            |
| POST   | `/api/admin/ai-training/training-jobs`           | SBERT 파인튜닝 및 Domain/Intent 분류기 학습 |
| POST   | `/api/admin/ai-training/evaluation-jobs`         | 학습 완료 모델 성능 평가 작업 생성              |

### 6.3 작업 조회 API

| Method | URI                              | 설명             |
| ------ | -------------------------------- | -------------- |
| GET    | `/api/admin/ai-training/jobs/{job_id}` | Job 상태 및 결과 조회 |

### 6.4 모델 관리 API

| Method | URI                                  | 설명                      |
| ------ | ------------------------------------ | ----------------------- |
| GET    | `/api/admin/ai-training/models`            | 저장된 모델 버전 목록 및 성능 요약 조회 |
| GET    | `/api/admin/ai-training/models/{model_id}` | 모델 상세 정보 조회             |
| PATCH  | `/api/admin/ai-training/models/{model_id}` | 특정 모델을 운영용(active)으로 전환 |

---

## 7. RabbitMQ 큐 스펙

### 7.1 요청 큐

| 항목   | 값                   |
| ---- | ------------------- |
| 큐 이름 | `q.2ai.training`    |
| 방향   | Backend → AI server |
| 용도   | 학습 Job 요청 전달        |

### 7.2 완료 이벤트 큐

| 항목   | 값                   |
| ---- | ------------------- |
| 큐 이름 | `q.2app.training`   |
| 방향   | AI server → Backend |
| 용도   | 학습 완료/실패 이벤트 전달     |

### 7.3 Exchange 사용 기준

현재 테스트 및 구현 기준으로 **default exchange (`""`)** 를 사용한다.
즉, routing key를 큐 이름과 동일하게 주는 방식이다.

예:

* 요청 발행: `routing_key = "q.2ai.training"`
* 완료 이벤트 수신 대상: `q.2app.training`

---

## 8. 요청 메시지 스펙

### 8.1 Backend → AI 요청 메시지

```json
{
  "job_id": "job_001",
  "job_type": "training",
  "task_type": "training",
  "dataset_version": "v3",
  "requested_by": "admin",
  "created_at": "2026-04-12T04:52:33.029Z"
}
```

### 8.2 필드 정의

| 필드명               | 타입               | 설명          |
| ----------------- | ---------------- | ----------- |
| `job_id`          | string           | Job 고유 식별자  |
| `job_type`        | string           | 작업 종류       |
| `task_type`       | string           | 실제 실행 작업 타입 |
| `dataset_version` | string           | 데이터셋 버전     |
| `requested_by`    | string           | 요청자         |
| `created_at`      | string(datetime) | Job 생성 시각   |

### 8.3 현재 사용 기준

현재 AI worker는 다음 값 조합을 기준으로 처리한다.

* `job_type = "training"`
* `task_type = "training"`

향후 preprocessing, pair, evaluation 으로 확장 가능하나,
현재 구현 및 검증 완료 범위는 training 기준이다.

---

## 9. 완료 이벤트 메시지 스펙

### 9.1 AI → Backend 완료 이벤트

```json
{
  "job_id": "job_001",
  "status": "completed",
  "model_version": "v2026_04_12_045539",
  "finished_at": "2026-04-12T04:55:39.498Z",
  "metrics": {
    "intent_f1": 0.0,
    "domain_accuracy": 0.0
  },
  "error_message": null
}
```

### 9.2 필드 정의

| 필드명             | 타입               | 설명                      |
| --------------- | ---------------- | ----------------------- |
| `job_id`        | string           | Job 식별자                 |
| `status`        | string           | `completed` 또는 `failed` |
| `model_version` | string           | 생성된 모델 버전               |
| `finished_at`   | string(datetime) | 작업 종료 시각                |
| `metrics`       | object           | 모델 성능 지표                |
| `error_message` | string/null      | 실패 시 에러 메시지             |

### 9.3 Backend 반영 기준

Backend는 `job_id`를 기준으로 기존 row를 찾아 update 해야 한다.

---

## 10. training_jobs 테이블 기준

현재 학습 Job 상태 저장용 테이블은 별도로 생성하는 것이 맞으며,
기존 `outbox` 테이블은 학습 Job 상태 추적 테이블을 대체하지 않는다.

### 10.1 테이블 목적

| 항목    | 설명              |
| ----- | --------------- |
| 테이블명  | `training_jobs` |
| 목적    | AI 학습 Job 상태 저장 |
| 기준 키  | `job_id`        |
| 사용 주체 | Backend         |

### 10.2 컬럼

| 컬럼명               | 타입           | 설명                                         |
| ----------------- | ------------ | ------------------------------------------ |
| `job_id`          | VARCHAR(64)  | Job ID, PK                                 |
| `job_type`        | VARCHAR(50)  | training / preprocessing / evaluation      |
| `task_type`       | VARCHAR(50)  | 실제 수행 작업                                   |
| `dataset_version` | VARCHAR(50)  | 데이터셋 버전                                    |
| `requested_by`    | VARCHAR(100) | 요청자                                        |
| `status`          | ENUM         | `QUEUED`, `RUNNING`, `COMPLETED`, `FAILED` |
| `model_version`   | VARCHAR(100) | 생성 모델 버전                                   |
| `metrics_json`    | LONGTEXT     | metrics JSON 저장                            |
| `error_message`   | TEXT         | 에러 메시지                                     |
| `created_at`      | DATETIME(6)  | Job 생성 시각                                  |
| `started_at`      | DATETIME(6)  | 작업 시작 시각                                   |
| `finished_at`     | DATETIME(6)  | 작업 종료 시각                                   |

### 10.3 상태 흐름

```text
QUEUED → RUNNING → COMPLETED
QUEUED → RUNNING → FAILED
```

### 10.4 초기 구현 권장 방식

초기에는 `RUNNING` 이벤트 없이 아래처럼 구현해도 된다.

```text
Job 생성 시: QUEUED
완료 이벤트 수신 시: COMPLETED / FAILED
```

이후 AI worker가 start 이벤트를 별도로 주면 `RUNNING` 확장 가능하다.

---

## 11. 이벤트 흐름 처리 기준

### 11.1 Job 생성 시 처리 흐름

```text
1. 관리자 웹에서 training 요청
2. Backend Controller 진입
3. job_id 생성
4. training_jobs insert
   - status = QUEUED
5. q.2ai.training publish
6. API 응답 반환
```

### 11.2 완료 이벤트 수신 시 처리 흐름

```text
1. Backend consumer가 q.2app.training consume
2. JSON 파싱
3. job_id 추출
4. training_jobs row 조회
5. status update
6. model_version 저장
7. metrics_json 저장
8. error_message 저장
9. finished_at 저장
10. 메시지 ack
```

### 11.3 실패 처리 기준

완료 이벤트의 `status = failed` 인 경우:

* `training_jobs.status = FAILED`
* `error_message` 저장
* `finished_at` 저장

---

## 12. Backend 구현 구조 예시

아래는 개념적인 구조 예시다. 실제 패키지명은 Backend 규칙에 맞게 조정 가능하다.

```text
backend/
 ├─ controller/
 │   └─ AiTrainingController
 ├─ service/
 │   └─ AiTrainingService
 ├─ messaging/
 │   ├─ TrainingJobProducer
 │   └─ TrainingResultConsumer
 ├─ repository/
 │   ├─ TrainingJobRepository
 │   └─ TrainedModelRepository
 ├─ domain/
 │   ├─ TrainingJob
 │   └─ TrainedModel
 └─ dto/
     ├─ TrainingJobCreateRequest
     ├─ TrainingJobResponse
     ├─ TrainingJobResultMessage
     └─ TrainingJobMessage
```

---

## 13. Backend Producer 구현 예시

### 13.1 역할

* `POST /api/ai-training/training-jobs` 요청 처리 후
* `q.2ai.training` 으로 Job 메시지 발행

### 13.2 개념 코드 예시

```java
public void publishTrainingJob(TrainingJobMessage message) {
    rabbitTemplate.convertAndSend(
        "",
        "q.2ai.training",
        message
    );
}
```

### 13.3 유의사항

* default exchange 사용
* routing key는 큐 이름과 동일
* DB insert 이후 publish 수행 권장

---

## 14. Backend Consumer 구현 예시

### 14.1 역할

* `q.2app.training` 결과 이벤트 consume
* `training_jobs` 업데이트

### 14.2 개념 코드 예시

```java
@RabbitListener(queues = "q.2app.training")
public void handleTrainingResult(String body) throws Exception {
    TrainingJobResultMessage result = objectMapper.readValue(body, TrainingJobResultMessage.class);

    TrainingJob job = trainingJobRepository.findByJobId(result.getJobId())
        .orElseThrow(() -> new IllegalArgumentException("Job not found: " + result.getJobId()));

    job.setStatus(result.getStatus().toUpperCase());
    job.setModelVersion(result.getModelVersion());
    job.setFinishedAt(result.getFinishedAt());
    job.setMetricsJson(objectMapper.writeValueAsString(result.getMetrics()));
    job.setErrorMessage(result.getErrorMessage());

    trainingJobRepository.save(job);
}
```

### 14.3 유의사항

* `job_id` 기준 update
* 파싱 실패 시 예외 처리 필요
* 상태값 casing 통일 필요 (`completed` → `COMPLETED`)
* metrics는 JSON string 저장 가능

---

## 15. Backend가 바로 구현해야 할 우선순위

### 15.1 1순위

* `training_jobs` 테이블 사용
* `POST /api/ai-training/training-jobs`
* `q.2ai.training` producer

### 15.2 2순위

* `q.2app.training` consumer
* DB 상태 업데이트

### 15.3 3순위

* `GET /api/ai-training/jobs/{job_id}`

### 15.4 4순위

* 모델 관리 API
* trained_models 테이블
* active model 전환

---

## 16. 최소 연동 테스트 시나리오

### 시나리오 1. Job 생성

1. Backend API 호출
2. `training_jobs` row 생성 확인
3. `status = QUEUED` 확인
4. `q.2ai.training` 메시지 발행 확인

### 시나리오 2. AI worker 처리

1. AI worker가 요청 consume
2. safe mode 또는 actual mode 실행
3. `q.2app.training` 완료 이벤트 발행 확인

### 시나리오 3. Backend 결과 반영

1. Backend consumer가 완료 이벤트 수신
2. `training_jobs` row update
3. `status = COMPLETED`
4. `model_version`, `metrics_json`, `finished_at` 저장 확인

### 시나리오 4. 실패 반영

1. 실패 이벤트 수신
2. `status = FAILED`
3. `error_message` 저장 확인

---

## 17. Backend 연동 시 주의사항

### 17.1 outbox 테이블과 training_jobs는 역할이 다름

현재 `outbox`는 메시지 발행용 아웃박스 역할에 가깝고,
학습 Job 상태 저장 테이블을 대체하지 않는다.

### 17.2 관리자 UI는 DB 기준으로 상태를 봐야 함

관리자 웹은 RabbitMQ를 직접 보지 않고,
반드시 Backend DB 상태를 기준으로 조회해야 한다.

### 17.3 초기에는 단순하게 가는 것이 좋음

처음부터 retry, DLQ, progress 이벤트, scheduler까지 넣지 말고
아래까지만 우선 완성하는 것이 가장 현실적이다.

* Job 생성
* 요청 메시지 발행
* 완료 이벤트 consume
* DB 상태 업데이트
* 상태 조회 API

### 17.4 현재 AI worker는 training 기준으로만 검증 완료

현재 safe mode 및 완료 이벤트 흐름이 검증된 범위는 `training` 기준이다.
`preprocessing`, `pair`, `evaluation`은 확장 가능하나, Backend는 먼저 training 흐름을 기준으로 연동하는 것이 좋다.

---

## 18. 최종 정리

현재 AI 서버는 이미 다음을 정상적으로 수행할 수 있다.

* `q.2ai.training` 요청 consume
* training worker 실행
* 완료 이벤트 생성
* `q.2app.training` 완료 이벤트 publish

따라서 Backend가 추가로 구현해야 할 핵심은 다음과 같다.

* `training_jobs` 기반 Job 상태 관리
* Job 생성 API
* `q.2ai.training` 요청 발행기
* `q.2app.training` 완료 이벤트 consumer
* 상태 조회 API

즉, Backend 연동의 핵심은
“MQ로 들어오는 AI 학습 요청과 완료 이벤트를 DB 중심으로 관리하고, 이를 REST API로 관리자 UI에 제공하는 것”이다.