package com.emailagent.dto.request.calendar;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CalendarEventRequest {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotNull(message = "시작 일시는 필수입니다.")
    private LocalDateTime startDatetime; // 프론트 요청: camelCase "startDatetime"

    private LocalDateTime endDatetime;

    private String eventType; // 프론트 요청: camelCase "eventType" (meeting/video/call/deadline)

    private String location;

    private String notes; // DB의 description 컬럼에 저장
}
