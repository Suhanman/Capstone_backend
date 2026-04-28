package com.emailagent.domain.enums;

public enum EmailStatus {
    PENDING_REVIEW,   // 검토 대기
    PROCESSED,        // 처리 완료
    AUTO_SENT,        // 자동 발송됨
    DELETED           // Gmail에서 삭제됨 (소프트 삭제 — 물리 레코드 유지)
}
