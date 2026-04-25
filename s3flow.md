# S3 파일 업로드 & RAG 지식 적재 플로우

## 개요

파일 업로드는 **3단계 핸드셰이크** 구조이며, App Server는 S3 데이터를 직접 수신하지 않는다.
업로드 완료 후 RAG 지식 적재 시점에 AI(RAG) Server가 Presigned GET URL로 S3 파일을 직접 내려받는다.

---

## Phase 1 — Presigned PUT URL 발급

```
Frontend
  │
  │  POST /api/business/resources/presigned-url
  │  Authorization: Bearer <JWT>
  │  Body: { "file_name": "manual.pdf", "content_type": "application/pdf" }
  ▼
BusinessController.getPresignedUrl()
  │  @CurrentUser → userId 추출
  ▼
BusinessService.generatePresignedUrl()
  │  1. 파일명 sanitize: replaceAll("[/\\\\]", "_")  ← 경로 순회 공격 방지
  │  2. s3Key 생성: "resources/{userId}/{UUID}_{safeFileName}"
  │  3. contentType 기본값: "application/octet-stream"
  ▼
S3Service.generatePresignedPutUrl(s3Key, contentType)
  │  S3Presigner → PutObjectPresignRequest
  │  서명 유효시간: 10분
  ▼
PresignedUrlResponse
  { "presigned_url": "https://s3.amazonaws.com/capstone-gachon/resources/{userId}/...",
    "s3_key":        "resources/{userId}/{UUID}_manual.pdf" }
  │
  └─→ Frontend
```

> App Server는 AWS에 HTTP 호출을 하지 않는다. **S3Presigner는 로컬에서 AWS Signature V4 서명만 생성**한다.

---

## Phase 2 — S3 직접 PUT 업로드 (Frontend → S3)

```
Frontend
  │
  │  PUT {presigned_url}
  │  Content-Type: application/pdf   ← Phase 1 요청 시 전달한 값과 반드시 일치
  │  Body: (파일 바이너리)
  ▼
AWS S3 (capstone-gachon 버킷)
  │  서명 검증 성공 → 오브젝트 저장
  │  Key: resources/{userId}/{UUID}_manual.pdf
  └─→ HTTP 200 OK (App Server 경유 없음)
```

> App Server를 통하지 않으므로 대역폭 절감 및 서버 부하 없음.  
> 서명 만료(10분) 이후 요청은 AWS가 403으로 거부한다.

---

## Phase 3 — 업로드 확정 (메타데이터 DB 저장)

```
Frontend
  │
  │  POST /api/business/resources/files
  │  Authorization: Bearer <JWT>
  │  Body: { "s3_key": "resources/{userId}/...", "file_name": "manual.pdf" }
  ▼
BusinessController.confirmUpload()
  ▼
BusinessService.confirmUpload()
  │  1. s3Key 소유권 검증:
  │     s3Key.startsWith("resources/{userId}/") → 불일치 시 400 예외
  │     (다른 사용자 파일 등록 차단)
  │  2. fileType 추출: 확장자 대문자 ("PDF")
  │  3. BusinessResource 엔티티 생성
  │     - filePath = s3Key  ← 이후 Presigned GET URL 생성에 사용
  │  4. resourceRepository.save()
  ▼
BusinessResourceResponse (201 Created)
  { "resource_id": 42, "file_name": "manual.pdf", "file_type": "PDF", ... }
  │
  └─→ Frontend
```

---

## Phase 4 — RAG 지식 적재 (AI Server로 전달)

온보딩 완료 또는 리소스 변경 트리거 시점에 아래 흐름이 시작된다.

### 4-1. 백엔드: Presigned GET URL 생성 + MQ 발행

```
RagIntegrationService.requestKnowledgeIngest(userId, faqs, resources)
  │
  │  resources 순회:
  ▼
S3Service.generatePresignedGetUrl(resource.getFilePath())
  │  S3Presigner → GetObjectPresignRequest
  │  서명 유효시간: 60분  ← MQ 처리 지연 고려
  │  반환: "https://s3.amazonaws.com/capstone-gachon/resources/{userId}/...?X-Amz-Signature=..."
  ▼
RagKnowledgeIngestRequestDTO 조립
  {
    "job_id":     "rag-ingest-{userId}-{UUID}",
    "request_id": "{UUID}",
    "user_id":    {userId},
    "payload": {
      "faqs": [ { "source_id": "faq-{id}", "question": "...", "answer": "..." } ],
      "manuals": [
        {
          "source_id":    "manual-{resourceId}",
          "title":        "manual.pdf",
          "file_name":    "manual.pdf",
          "presigned_url": "https://s3.amazonaws.com/..."  ← AI Server가 이 URL로 직접 다운로드
        }
      ]
    }
  }
  ▼
RagPublisher.publishKnowledgeIngest()
  │  1. ragJobService.createKnowledgeIngestJob() → training_jobs 테이블에 PENDING 상태 저장
  │  2. rabbitTemplate.convertAndSend(
  │       exchange: "x.app2rag.direct",
  │       routingKey: "2rag.knowledge.ingest",
  │       payload: RagKnowledgeIngestRequestDTO
  │     )
  ▼
RabbitMQ: q.2rag.knowledge.ingest
```

### 4-2. RAG AI Server: 파일 처리

```
RAG AI Server (AWS EC2, WireGuard VPN 경유)
  │
  │  q.2rag.knowledge.ingest consume
  ▼
  payload.manuals[].presigned_url 로 S3 직접 HTTP GET
  │  (AI Server → AWS S3, App Server 경유 없음)
  ▼
  PDF 파싱 → chunking → embedding → Chroma 벡터 DB 저장
  │
  │  [진행 중] q.2app.rag.progress publish
  │    { "job_id": "...", "status": "RUNNING", "progress_step": "chunking", ... }
  │
  └─ [완료/실패] q.2app.knowledge.ingest publish
       { "job_id": "...", "status": "COMPLETED" | "FAILED", ... }
```

### 4-3. 백엔드: 결과 수신 및 상태 갱신

```
RabbitMQ: q.2app.rag.progress
  ▼
RagProgressConsumer (App Server)
  │  job_id 기준으로 training_jobs 테이블 갱신
  │  progress_step / progress_message / payload_json 업데이트
  └─→ SSE 브로드캐스트 또는 polling API 노출

RabbitMQ: q.2app.knowledge.ingest
  ▼
RagConsumer (App Server)
  │  job_id 기준으로 training_jobs 상태 COMPLETED / FAILED 확정
  └─→ Frontend는 SSE 또는 polling으로 완료 감지
```

---

## 전체 시퀀스 요약

```
Frontend         App Server        AWS S3       RabbitMQ       RAG AI Server
   │                  │               │              │               │
   │──[1] POST presigned-url──▶│               │              │
   │◀──presigned_url + s3_key──│               │              │
   │                  │               │              │               │
   │──[2] PUT {presigned_url}──────────▶│              │
   │◀──────────────── 200 OK ──────────│              │               │
   │                  │               │              │               │
   │──[3] POST /files──────────▶│               │              │
   │     (s3_key, file_name)    │── DB 저장 ──▶│              │
   │◀──── 201 resource ────────│               │              │
   │                  │               │              │               │
   │    (트리거 시점)  │               │              │               │
   │                  │── Presigned GET URL 생성 ──▶│(서명만, HTTP 없음)
   │                  │──[4] publish ──────────────▶│               │
   │                  │  q.2rag.knowledge.ingest     │               │
   │                  │               │              │──consume──────▶│
   │                  │               │◀─────────────────── GET file ─│
   │                  │               │──────────── 파일 바이너리 ───▶│
   │                  │               │              │  (처리 중)     │
   │                  │◀── progress ──────────────────│               │
   │◀── SSE/polling ──│               │              │               │
   │                  │◀── completed ─────────────────│               │
```

---

## 주요 보안 포인트

| 항목 | 내용 |
|------|------|
| PUT URL 유효시간 | 10분 (S3Presigner, 서버 HTTP 호출 없음) |
| GET URL 유효시간 | 60분 (MQ 처리 지연 고려) |
| 파일명 sanitize | `[/\\]` → `_` 치환으로 디렉터리 순회 방지 |
| s3Key 소유권 검증 | confirmUpload 시 `resources/{userId}/` prefix 강제 확인 |
| 파일 삭제 | `s3Service.deleteObject(resource.getFilePath())` 후 DB 레코드 삭제 |

---

## 관련 클래스 위치

| 역할 | 클래스 |
|------|--------|
| Presigned URL 발급 엔드포인트 | `controller/BusinessController.java:56` |
| URL 발급 비즈니스 로직 | `service/BusinessService.java:75` |
| 업로드 확정 로직 | `service/BusinessService.java:96` |
| S3 서명 유틸 | `service/S3Service.java` |
| S3 빈 설정 | `config/S3Config.java` |
| RAG 연동 파사드 | `rag/application/RagIntegrationService.java:33` |
| MQ 발행 | `rabbitmq/publisher/RagPublisher.java:29` |
| Knowledge Ingest DTO | `rabbitmq/dto/RagKnowledgeIngestRequestDTO.java` |
