---

# AI 재학습 & 무중단 배포 전환을 위한 백엔드 수정 가이드

## 0. 목적

본 문서는 현재 구현된 AI 학습 Job 시스템을
**최종 확정된 “무중단 배포 기반 MLOps 구조”에 맞게 정렬하기 위한 수정 사항을 정의**한다.

현재 시스템은 Job 기반 구조로 잘 설계되어 있으나,
다음 항목들이 최종 구조와 불일치하여 수정이 필요하다:

* 배포(Deployment)가 단순 DB 상태 변경으로 처리됨
* Dataset collection이 실제 데이터 export를 수행하지 않음
* 프론트 트리거 구조(3버튼)와 API 구조가 불일치
* Job 상태가 배포 과정 표현에 부족함

---

# 1. 최종 구조 기준 (변경 기준)

## 1.1 프론트 트리거 구조 (확정)

프론트는 아래 3개의 버튼만 사용한다.

| 기능      | 설명             |
| ------- | -------------- |
| 데이터 재수집 | 학습용 dataset 생성 |
| 재학습     | 모델 학습 수행       |
| 재배포     | 모델 교체 (무중단)    |

---

## 1.2 전체 파이프라인

```text
[Frontend]
   ↓
[Backend] → Job 생성 및 상태 관리
   ↓
[Dataset Collection Job]
   ↓
[S3 dataset 저장]
   ↓
[Training Job (GPU)]
   ↓
[S3 model 저장]
   ↓
[Deployment Job]
   ↓
[Inference Server]
   - preload
   - validate
   - active model switch
```

---

# 2. 반드시 수정해야 하는 항목 (Critical)

---

## 2.1 Deployment Job 추가 (필수)

### 현재 문제

* `PATCH /models/{model_id}` 로 ACTIVE 변경
* 실제 모델 로딩/검증 없이 DB만 변경됨

→ 무중단 배포 구조와 불일치

---

### 변경 요구사항

### 신규 API 추가

```http
POST /api/admin/ai-training/deployment-jobs
```

### 요청 예시

```json
{
  "model_version": "v2026_04_20_002"
}
```

---

### 동작 정의

1. deployment job 생성 (DB 저장)

2. inference 서버에 배포 요청 전달

3. inference 서버가 수행:

    * S3에서 모델 다운로드
    * 모델 로딩
    * 샘플 검증
    * active model switch

4. 성공 시:

    * 해당 model_version → ACTIVE

5. 실패 시:

    * 기존 모델 유지

---

### 핵심 변경

❗ 기존 방식 제거 또는 내부용으로만 유지

```http
PATCH /models/{model_id}
```

→ 외부 배포 API로 사용 금지

---

## 2.2 dataset-collections 기능 확장 (필수)

### 현재 문제

* dataset-collections는 단순 메타데이터 생성
* 실제 데이터 export 없음

---

### 변경 요구사항

dataset-collections를 **실제 데이터 생성 Job으로 변경**

---

### 변경 후 동작

```text
POST /dataset-collections
    ↓
Job 생성
    ↓
DB에서 이메일 데이터 조회
    ↓
JSONL 생성
    ↓
gzip 압축
    ↓
manifest 생성
    ↓
S3 업로드
    ↓
dataset_version READY
```

---

### 핵심 요구

* dataset_version = 실제 S3 데이터와 연결
* 단순 count 기반 metadata 금지

---

## 2.3 ACTIVE 모델 전환 시점 변경 (필수)

### 현재 문제

* DB에서 ACTIVE 먼저 변경
* inference 서버 반영 여부와 무관

---

### 변경 요구사항

ACTIVE 전환은 **inference 서버 성공 이후** 수행

---

### 변경 후 순서

```text
1. deployment job 생성
2. inference 서버 deploy 요청
3. 모델 preload
4. 모델 검증
5. active model switch (서버 내부)
6. 성공 응답
7. 그 이후 DB ACTIVE 변경
```

---

### 절대 금지

```text
DB ACTIVE 먼저 변경 → 나중에 서버 반영
```

---

## 2.4 Job 타입 확장 (필수)

### 현재

```text
preprocessing
pair
training
evaluation
```

---

### 추가 필요

```text
dataset_collection
deployment
```

---

### 목적

* 모든 작업을 Job 기반으로 통합 관리

---

## 2.5 Job 상태 세분화 (필수)

최소 요구 상태:

```text
QUEUED
MODEL_LOADING
SWITCHING
COMPLETED
FAILED
```

---

### 이유

프론트에서 상태 표시 필요:

* 다운로드 중
* 로딩 중
* 교체 중

---

### 권장 확장 (선택)

```text
MODEL_DOWNLOADING
MODEL_LOADING
MODEL_VALIDATING
SWITCHING
```

---

## 2.6 Job 조회 API 응답 확장 (필수)

### 현재 문제

* 상태 외 정보 부족

---

### 요구 응답 구조

```json
{
  "job_id": "job_001",
  "job_type": "deployment",
  "status": "MODEL_LOADING",
  "dataset_version": null,
  "model_version": "v2026_04_20_002",
  "progress": 60,
  "error_message": null,
  "created_at": "...",
  "started_at": "...",
  "finished_at": null
}
```

---

### 필수 필드

* job_type
* status
* dataset_version
* model_version
* progress
* error_message

---

# 3. 구조 변경 권고 (Recommended)

---

## 3.1 외부 API 단순화

### 현재

* preprocessing, pair 등 외부 노출

---

### 권고

프론트에서는 아래만 사용:

```http
POST /dataset-collections
POST /training-jobs
POST /deployment-jobs
GET /jobs/{job_id}
GET /models
```

---

## 3.2 내부 파이프라인 유지

아래는 내부 단계로 유지:

* preprocessing
* pair 생성
* evaluation

---

## 3.3 Job 테이블 명칭

현재: `training_jobs`

권고:

* 그대로 사용 가능
* 의미는 “AI 작업 전체”로 확장

---

## 3.4 Deployment 처리 방식

권장 구조:

```text
Backend → MQ or HTTP → Inference Server
```

Inference 서버가 직접 수행:

* 모델 다운로드
* 로딩
* 검증
* switch

---

# 4. 변경 후 전체 흐름

```text
[Frontend]

1. dataset-collections
2. training-jobs
3. deployment-jobs

↓

[Backend]

Job 생성
상태 관리

↓

[Worker / Training]

S3 dataset 생성
모델 학습

↓

[S3]

dataset / model 저장

↓

[Deployment Job]

Inference Server 호출

↓

[Inference Server]

preload
validate
switch

↓

[Backend]

ACTIVE 반영
```

---

# 5. 핵심 요약

---

## 반드시 수정

* deployment-job 추가
* dataset export 실제 구현
* ACTIVE 변경 시점 수정
* job_type 확장
* job 상태 확장
* job 조회 응답 확장

---

## 권고

* 외부 API 단순화
* 내부 파이프라인 숨김
* inference 서버 중심 배포 구조

---

# 6. 한 줄 결론

> 현재 구조는 유지하면서, 배포를 Job으로 승격하고 dataset을 실제 데이터 기반으로 변경하며, ACTIVE 전환을 inference 서버 성공 이후로 이동하는 것이 핵심 수정 사항이다.

---