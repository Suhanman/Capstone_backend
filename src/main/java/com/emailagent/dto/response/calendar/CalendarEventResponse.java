package com.emailagent.dto.response.calendar;

import com.emailagent.domain.entity.CalendarEvent;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 캘린더 이벤트 목록 아이템 DTO (list item 용, BaseResponse 상속 없음) */
@Getter
@Builder
public class CalendarEventResponse {

    @JsonProperty("event_id")
    private Long eventId;

    private String title;

    @JsonProperty("start_datetime")
    private LocalDateTime startDatetime;

    @JsonProperty("end_datetime")
    private LocalDateTime endDatetime;

    private String source;
    private String status;

    @JsonProperty("is_calendar_added")
    private boolean isCalendarAdded;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static CalendarEventResponse from(CalendarEvent event) {
        return CalendarEventResponse.builder()
                .eventId(event.getEventId())
                .title(event.getTitle())
                .startDatetime(event.getStartDatetime())
                .endDatetime(event.getEndDatetime())
                .source(event.getSource())
                .status(event.getStatus())
                .isCalendarAdded(event.isCalendarAdded())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
