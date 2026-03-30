package com.emailagent.dto.response.calendar;

import com.emailagent.dto.response.auth.BaseResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CalendarEventListResponse extends BaseResponse {

    private List<CalendarEventResponse> data;
}
