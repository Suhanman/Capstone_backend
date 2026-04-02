# 1. Users

서비스의 **기본 사용자 계정 정보**를 저장하는 테이블

```sql
CREATE TABLE Users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 사용자 ID
    email VARCHAR(255) NOT NULL UNIQUE, -- 로그인 이메일
    password VARCHAR(255) NOT NULL, -- 로그인 비밀번호
    name VARCHAR(100) NOT NULL, -- 사용자 이름
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER', -- 사용자 권한
    is_active TINYINT(1) NOT NULL DEFAULT 1, -- 활성 여부
    last_login_at DATETIME NULL, -- 최근 로그인

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP -- 수정 일시
);
```

# 2. Integrations

사용자가 연결한 **외부 계정 연동 정보**를 저장하는 테이블

```sql
CREATE TABLE Integrations (
    integration_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 연동 ID
    user_id BIGINT NOT NULL UNIQUE, -- 사용자 1명당 Google 계정 연동 1건
    provider VARCHAR(50) NOT NULL DEFAULT 'GOOGLE', -- 연동 제공자
    connected_email VARCHAR(255) NOT NULL, -- 연동된 Google 이메일
    external_account_id VARCHAR(255) NULL, -- Google 계정 고유 식별자(sub 등)
    
    access_token TEXT, -- Google OAuth access token
    refresh_token TEXT, -- Google OAuth refresh token
    token_expires_at DATETIME NULL, -- Google OAuth access token 만료 시각
    
    granted_scopes TEXT NULL, -- 사용자가 실제로 승인한 Google 권한 목록
    is_gmail_connected TINYINT(1) NOT NULL DEFAULT 0, -- [추가됨] Gmail 연동 상태 (0: 미연동, 1: 연동)
    is_calendar_connected TINYINT(1) NOT NULL DEFAULT 0, -- [추가됨] Calendar 연동 상태 (0: 미연동, 1: 연동)
    
    sync_status ENUM('CONNECTED', 'DISCONNECTED', 'ERROR') NOT NULL DEFAULT 'CONNECTED', -- 현재 Google 연동 상태(정상/해제/오류)
    last_synced_at DATETIME NULL, -- 마지막 동기화 시각
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시
    
    CONSTRAINT fk_integrations_user
      FOREIGN KEY (user_id) REFERENCES Users(user_id)
          ON DELETE CASCADE
);
```

# 3. BusinessProfiles

```sql
CREATE TABLE BusinessProfiles (
    profile_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 프로필 ID
    user_id BIGINT NOT NULL UNIQUE, -- 사용자 ID
    industry_type VARCHAR(100), -- 업종 유형
    email_tone ENUM('FORMAL', 'NEUTRAL', 'FRIENDLY') DEFAULT 'NEUTRAL', -- 이메일 어조
    company_description TEXT, -- 회사 설명

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시

    CONSTRAINT fk_businessprofiles_user
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
);
```

# 4. BusinessResources

AI가 참고할 **비즈니스 자료**를 저장하는 테이블

```sql
CREATE TABLE BusinessResources (
    resource_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 자료 ID
    user_id BIGINT NOT NULL, -- 사용자 ID
    title VARCHAR(255), -- 자료 제목
    file_name VARCHAR(255) NOT NULL, -- 파일명
    file_path VARCHAR(500) NOT NULL, -- 파일 경로
    file_type VARCHAR(50), -- 파일 형식
    extracted_text LONGTEXT NULL; -- 파일 내용 추출 후 저장 - 3/22 추가📍

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시

    CONSTRAINT fk_businessresources_user
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
);
```

# 5. BusinessFAQs

```sql
CREATE TABLE BusinessFAQs (
    faq_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- FAQ ID
    user_id BIGINT NOT NULL, -- 사용자 ID
    question VARCHAR(500) NOT NULL, -- 질문
    answer TEXT NOT NULL, -- 답변

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시

    CONSTRAINT fk_businessfaqs_user
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
);
```

# 6. Categories

```sql
CREATE TABLE Categories (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 카테고리 ID
    user_id BIGINT NOT NULL, -- 사용자 ID
    category_name VARCHAR(100) NOT NULL, -- 카테고리명
    color VARCHAR(30), -- 표시 색상

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시

    CONSTRAINT uq_categories_user_name
        UNIQUE (user_id, category_name),
    CONSTRAINT fk_categories_user
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
);
```

# 7. Templates

카테고리 별로 사용할 **답장 템플릿**을 저장하는 테이블

```sql
CREATE TABLE Templates (
    template_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 템플릿 ID
    user_id BIGINT NOT NULL, -- 사용자 ID
    category_id BIGINT NOT NULL, -- 카테고리 ID
    title VARCHAR(255) NOT NULL, -- 템플릿 제목
    subject_template VARCHAR(500) NOT NULL, -- 제목 양식
    body_template TEXT NOT NULL, -- 본문 양식
    accuracy_score DECIMAL(5,2), -- 정확도 점수

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시

    CONSTRAINT fk_templates_user
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_templates_category
        FOREIGN KEY (category_id) REFERENCES Categories(category_id)
        ON DELETE CASCADE
);
```

# 8. Emails

사용자가 받은 **이메일 원문과 분석 결과를 함께 저장하는 테이블**

```sql
CREATE TABLE Emails (
    email_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    external_msg_id VARCHAR(128) NOT NULL UNIQUE,

    sender_name VARCHAR(100),
    sender_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,

    body_raw LONGTEXT,         -- 원문 본문(HTML/plain 포함 원본 기준)
    body_clean LONGTEXT NOT NULL, -- AI 분석용 정제 본문

    received_at DATETIME NOT NULL,

    status ENUM('PENDING_REVIEW', 'PROCESSED', 'AUTO_SENT') NOT NULL DEFAULT 'PENDING_REVIEW',
    importance_level ENUM('LOW', 'MEDIUM', 'HIGH') NOT NULL DEFAULT 'MEDIUM',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_emails_user
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
);
```

# 9. TemplateUsageLogs

```sql
CREATE TABLE TemplateUsageLogs (
    usage_log_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 로그 ID
    template_id BIGINT NOT NULL, -- 템플릿 ID
    email_id BIGINT, -- 이메일 ID
    user_id BIGINT NOT NULL, -- 사용자 ID
    action_type ENUM('MATCHED', 'USED', 'EDITED') NOT NULL, -- 사용 유형
    action_detail TEXT, -- 상세 내용
    variables_info JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시

    CONSTRAINT fk_templateusagelogs_template
        FOREIGN KEY (template_id) REFERENCES Templates(template_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_templateusagelogs_email
        FOREIGN KEY (email_id) REFERENCES Emails(email_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_templateusagelogs_user
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
);
```

# 10. EmailAnalysisResults

```sql
CREATE TABLE EmailAnalysisResults (
    analysis_result_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 분석 결과 ID
    email_id BIGINT NOT NULL UNIQUE, -- 이메일 ID
    category_id BIGINT, -- 카테고리 ID
    domain VARCHAR(100) NULL, -- 도메인 분류
    intent VARCHAR(100), -- 인텐트
    confidence_score DECIMAL(5,2), -- 분류 신뢰도
    schedule_detected TINYINT(1) NOT NULL DEFAULT 0, -- 일정 감지 여부
    summary_text TEXT, -- 요약 내용
    entities_json JSON NULL, -- 추출 엔티티
    model_version VARCHAR(50) NULL, -- 모델 버전

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시

    CONSTRAINT fk_emailanalysisresults_email
        FOREIGN KEY (email_id) REFERENCES Emails(email_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_emailanalysisresults_category
        FOREIGN KEY (category_id) REFERENCES Categories(category_id)
        ON DELETE SET NULL
);
```

# 11. CalendarEvents

캘린더 화면에 표시될 **일정 정보**를 저장하는 테이블

```sql
CREATE TABLE CalendarEvents (
    calendar_event_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 일정 ID
    user_id BIGINT NOT NULL, -- 사용자 ID
    email_id BIGINT NULL, -- 이메일 ID

    title VARCHAR(255) NOT NULL, -- 일정 제목
    start_datetime DATETIME NOT NULL, -- 시작 일시
    end_datetime DATETIME, -- 종료 일시
    location VARCHAR(255), -- 일정 장소
    participants VARCHAR(500), -- 참석자 정보
    event_type VARCHAR(50), -- 일정 유형 (예: 대면 미팅, 화상 회의 등)
    description TEXT, -- 메모 및 상세 내용
    
    source ENUM('EMAIL', 'MANUAL', 'SYNC') NOT NULL DEFAULT 'EMAIL', -- 생성 출처
    status ENUM('PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'PENDING', -- 일정 상태
    is_calendar_added TINYINT(1) NOT NULL DEFAULT 0, -- 캘린더 등록 여부

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시

    CONSTRAINT fk_calendarevents_user
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_calendarevents_email
        FOREIGN KEY (email_id) REFERENCES Emails(email_id)
        ON DELETE SET NULL
);
```

# 12. DraftReplies

이메일에 대해 생성된 **답장 초안**을 저장하는 테이블

```sql
CREATE TABLE DraftReplies (
    draft_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 초안 ID
    user_id BIGINT NOT NULL, -- 사용자 ID
    email_id BIGINT NOT NULL, -- 이메일 ID
    template_id BIGINT NULL, -- 템플릿 ID

    subject VARCHAR(500) NOT NULL, -- 답장 제목
    body LONGTEXT NOT NULL, -- 답장 본문
    status ENUM('PENDING_REVIEW', 'EDITED', 'SENT', 'SKIPPED') NOT NULL DEFAULT 'PENDING_REVIEW', -- 초안 상태

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
    sent_at DATETIME NULL, -- 발송 일시
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시

    CONSTRAINT fk_draftreplies_user
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_draftreplies_email
        FOREIGN KEY (email_id) REFERENCES Emails(email_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_draftreplies_template
        FOREIGN KEY (template_id) REFERENCES Templates(template_id)
        ON DELETE SET NULL
);
```

# 13. AutomationRules

사용자가 설정한 **자동화 규칙**을 저장하는 테이블

```sql
CREATE TABLE AutomationRules (
    rule_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 규칙 ID
    user_id BIGINT NOT NULL, -- 사용자 ID
    category_id BIGINT NOT NULL, -- 카테고리 ID
    template_id BIGINT NULL, -- 템플릿 ID
    keywords JSON NOT NULL, -- 매칭 키워드
    auto_send_enabled TINYINT(1) NOT NULL DEFAULT 0, -- 자동 발송 여부
    auto_calendar_enabled TINYINT(1) NOT NULL DEFAULT 0, -- 자동 일정 등록
    is_active TINYINT(1) NOT NULL DEFAULT 1, -- 활성 여부

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시

    CONSTRAINT fk_automationrules_user
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_automationrules_category
        FOREIGN KEY (category_id) REFERENCES Categories(category_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_automationrules_template
        FOREIGN KEY (template_id) REFERENCES Templates(template_id)
        ON DELETE SET NULL
);
```

# 14. Notifications

사용자에게 보여줄 **알림 정보**를 저장하는 테이블

```sql
CREATE TABLE Notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 알림 ID
    user_id BIGINT NOT NULL, -- 사용자 ID

    type ENUM(
        'NEW_EMAIL',
        'DRAFT_PENDING',
        'EMAIL_DISCONNECTED',
        'UNCLASSIFIED_EMAIL',
        'EVENT_PENDING',
        'AUTO_SEND_SUMMARY'
    ) NOT NULL, -- 알림 유형

    title VARCHAR(255) NOT NULL, -- 알림 제목
    message TEXT NOT NULL, -- 알림 내용
    related_id BIGINT NULL, -- 관련 대상 ID
    is_read TINYINT(1) NOT NULL DEFAULT 0, -- 읽음 여부

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시

    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
);
```

# 15. SupportTickets

사용자가 관리자에게 보내는 **문의와 답변**을 저장하는 테이블

```sql
CREATE TABLE SupportTickets (
    ticket_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 문의 ID
    user_id BIGINT NOT NULL, -- 사용자 ID

    title VARCHAR(255) NOT NULL, -- 문의 제목
    content TEXT NOT NULL, -- 문의 내용
    status ENUM('PENDING', 'ANSWERED') NOT NULL DEFAULT 'PENDING', -- 처리 상태

    admin_reply TEXT, -- 관리자 답변
    replied_by BIGINT NULL, -- 답변 관리자 ID
    replied_at DATETIME NULL, -- 답변 일시

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 수정 일시

    CONSTRAINT fk_supporttickets_user
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_supporttickets_admin
        FOREIGN KEY (replied_by) REFERENCES Users(user_id)
        ON DELETE SET NULL
);
```

# 16. Outbox

메시지 큐(RabbitMQ) 기반 AI 파이프라인의 데이터 전송 상태를 추적하고, 관리자 모니터링 및 전송 실패 시 재시도를 관리하는 테이블
CREATE TABLE Outbox (
    outbox_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 아웃박스 ID
    email_id BIGINT NOT NULL, -- 이메일 ID

    status ENUM('READY', 'SENDING', 'FINISH', 'FAILED') NOT NULL DEFAULT 'READY', -- 파이프라인 상태
    payload JSON NOT NULL, -- 전송 페이로드 (email_id, 본문 데이터 등)
    retry_count TINYINT NOT NULL DEFAULT 0, -- 현재 재시도 횟수
    max_retry TINYINT NOT NULL DEFAULT 5, -- 최대 허용 재시도 횟수
    fail_reason VARCHAR(500) NULL, -- 최종 실패 사유 (원인 추적용)

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 일시 (READY 상태)
    sent_at DATETIME NULL, -- 발송 일시 (SENDING 전환 시각)
    finished_at DATETIME NULL, -- 처리 완료 일시 (FINISH 전환 시각)

    CONSTRAINT fk_outbox_email
        FOREIGN KEY (email_id) REFERENCES Emails(email_id)
        ON DELETE CASCADE,

    -- 모니터링 및 상태 폴링(Polling)을 위한 인덱스 최적화
    INDEX idx_outbox_status (status),
    INDEX idx_outbox_email_id (email_id),
    INDEX idx_outbox_status_created (status, created_at), -- Relay 폴링용
    INDEX idx_outbox_sent_at (sent_at) -- 타임아웃 감지용
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;