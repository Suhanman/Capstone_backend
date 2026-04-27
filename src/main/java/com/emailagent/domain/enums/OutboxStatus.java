package com.emailagent.domain.enums;
public enum OutboxStatus {
    READY,      // 발행 대기
    SENDING,    // AI 서버 발행 중
    FINISH,     // 처리 완료
    FAILED,     // AI 오류 또는 재시도 한도 초과
    CANCELLED   // 사용자가 Gmail에서 삭제하여 처리 취소됨 (FAILED와 원인 구분용)
}
