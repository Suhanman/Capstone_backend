package com.emailagent.dto.response.dashboard;

import com.emailagent.domain.entity.CalendarEvent;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ScheduleResponse extends BaseResponse {

    private List<ScheduleItem> data;

    @Getter
    @Builder
    public static class ScheduleItem {

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

        public static ScheduleItem from(CalendarEvent event) {
            return ScheduleItem.builder()
                    .eventId(event.getEventId())
                    .title(event.getTitle())
                    .startDatetime(event.getStartDatetime())
                    .endDatetime(event.getEndDatetime())
                    .source(event.getSource())
                    .status(event.getStatus())
                    .isCalendarAdded(event.isCalendarAdded())
                    .build();
        }
    }
}
