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

    @JsonProperty("event_type")
    private String eventType;

    private String location;

    private String notes; // DB description 컬럼 → notes 노출

    private String source;
    private String status;

    @JsonProperty("is_calendar_added")
    private boolean isCalendarAdded;

    @JsonProperty("email_id")
    private Long emailId;

    // 원본 이메일 연결 정보 (이메일에서 감지된 일정인 경우에만 값 존재)
    @JsonProperty("email_sender_name")
    private String emailSenderName;

    @JsonProperty("email_subject")
    private String emailSubject;

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
                .eventType(event.getEventType())
                .location(event.getLocation())
                .notes(event.getDescription())
                .source(event.getSource())
                .status(event.getStatus())
                .isCalendarAdded(event.isCalendarAdded())
                .emailId(event.getEmail() != null ? event.getEmail().getEmailId() : null)
                .emailSenderName(event.getEmail() != null ? event.getEmail().getSenderName() : null)
                .emailSubject(event.getEmail() != null ? event.getEmail().getSubject() : null)
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
