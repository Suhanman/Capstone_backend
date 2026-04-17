이제 AI 연동 문서에 명시된 마지막 남은 미구현 API들을 모두 완성해서 프로젝트를 마무리해 보자.

[구현 대상 API]

1. 데이터셋 관리 API



GET /api/admin/ai-training/datasets (등록된 데이터셋 버전, 건수, 상태 목록 조회)

POST /api/admin/ai-training/dataset-collections (원천 데이터 기반 학습용 데이터 재수집 Job 생성)

2. 전처리 및 평가 작업 API

(이 API들은 새로 테이블을 만들지 말고, 우리가 1~2순위에서 만든 training_jobs 테이블과 TrainingJobPublisher를 그대로 재사용해 줘. 내부적으로 job_type과 task_type만 각각의 용도에 맞게 변경해서 MQ(x.app2ai.direct, routing_key: 2ai.training)로 던지면 돼.)



POST /api/admin/ai-training/preprocessing-jobs (결측 제거, 공백 정리 작업)

POST /api/admin/ai-training/pair-jobs (SBERT 파인튜닝용 pair 생성 작업)

POST /api/admin/ai-training/evaluation-jobs (학습 완료 모델 성능 평가 작업)

[주의사항 및 요구사항]



기존에 만든 AiTrainingController와 AiTrainingService에 이 기능들을 추가해 줘. (코드가 너무 길어지면 Service를 분리해도 좋아.)

응답은 data 래퍼 객체 없는 Flat JSON 구조를 엄격하게 유지해 줘.

POST /api/admin/ai-training/dataset-collections (데이터 재수집) 기능도 결국 비동기 Job이라면, 이것도 MQ를 타야 하는지 아니면 단순 DB 기록인지 너의 아키텍처 판단을 덧붙여서 코드를 작성해 줘.