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

    @JsonProperty("event_type")
    private String eventType;

    private String location;

    private String source;
    private String status;

    @JsonProperty("is_calendar_added")
    private boolean isCalendarAdded;

    @JsonProperty("email_id")
    private Long emailId;

    @JsonProperty("email_sender_name")
    private String emailSenderName;

    @JsonProperty("email_subject")
    private String emailSubject;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static CalendarEventResponse from(CalendarEvent event) {
        return CalendarEventResponse.builder()
                .eventId(event.getEventId())
                .title(event.getTitle())
                .startDatetime(event.getStartDatetime())
                .endDatetime(event.getEndDatetime())
                .eventType(event.getEventType())
                .location(event.getLocation())
                .source(event.getSource())
                .status(event.getStatus())
                .isCalendarAdded(event.isCalendarAdded())
                .emailId(event.getEmail() != null ? event.getEmail().getEmailId() : null)
                .emailSenderName(event.getEmail() != null ? event.getEmail().getSenderName() : null)
                .emailSubject(event.getEmail() != null ? event.getEmail().getSubject() : null)
                .createdAt(event.getCreatedAt())
                .build();
    }
}
