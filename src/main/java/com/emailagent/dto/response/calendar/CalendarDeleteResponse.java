package com.emailagent.dto.response.calendar;

import com.emailagent.dto.response.auth.BaseResponse;

/**
 * DELETE /api/calendar/events/{event_id} 응답
 * 공통 필드(success, result_code, result_req)만 반환하고 data=null 포함
 */
public class CalendarDeleteResponse extends BaseResponse {

    private final Object data = null;

    public Object getData() {
        return data;
    }

    public static final CalendarDeleteResponse OK = new CalendarDeleteResponse();
}
