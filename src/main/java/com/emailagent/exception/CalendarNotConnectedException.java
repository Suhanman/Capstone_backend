package com.emailagent.exception;

/**
 * 캘린더 권한(is_calendar_connected=false)이 없는 사용자가 캘린더 관련 API를 호출할 때 발생.
 * GlobalExceptionHandler에서 HTTP 403 + error_code: CALENDAR_NOT_CONNECTED 로 응답한다.
 */
public class CalendarNotConnectedException extends RuntimeException {

    public CalendarNotConnectedException() {
        super("캘린더 접근 권한이 필요합니다. (CALENDAR_NOT_CONNECTED)");
    }
}
