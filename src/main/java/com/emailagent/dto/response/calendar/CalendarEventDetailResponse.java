package com.emailagent.dto.response.calendar;

import com.emailagent.domain.entity.CalendarEvent;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CalendarEventDetailResponse extends BaseResponse {

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

    @JsonProperty("email_id")
    private Long emailId;       // 이메일에서 감지된 경우 연결된 이메일 ID (nullable)

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public static CalendarEventDetailResponse from(CalendarEvent event) {
        return CalendarEventDetailResponse.builder()
                .eventId(event.getEventId())
                .title(event.getTitle())
                .startDatetime(event.getStartDatetime())
                .endDatetime(event.getEndDatetime())
                .source(event.getSource())
                .status(event.getStatus())
                .isCalendarAdded(event.isCalendarAdded())
                .emailId(event.getEmail() != null ? event.getEmail().getEmailId() : null)
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
