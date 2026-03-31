package com.emailagent.controller;

import com.emailagent.dto.request.calendar.CalendarEventRequest;
import com.emailagent.dto.response.calendar.CalendarDeleteResponse;
import com.emailagent.dto.response.calendar.CalendarEventDetailResponse;
import com.emailagent.dto.response.calendar.CalendarEventListResponse;
import com.emailagent.security.CurrentUser;
import com.emailagent.service.CalendarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    // GET /api/calendar/events?start_date=&end_date=
    @GetMapping("/events")
    public ResponseEntity<CalendarEventListResponse> getEvents(
            @CurrentUser Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start_date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end_date) {
        return ResponseEntity.ok(calendarService.getEvents(userId, start_date, end_date));
    }

    // GET /api/calendar/events/{event_id}
    @GetMapping("/events/{eventId}")
    public ResponseEntity<CalendarEventDetailResponse> getEvent(
            @CurrentUser Long userId,
            @PathVariable Long eventId) {
        return ResponseEntity.ok(calendarService.getEvent(userId, eventId));
    }

    // POST /api/calendar/events
    @PostMapping("/events")
    public ResponseEntity<CalendarEventDetailResponse> createEvent(
            @CurrentUser Long userId,
            @Valid @RequestBody CalendarEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(calendarService.createEvent(userId, request));
    }

    // PATCH /api/calendar/events/{event_id}/confirm
    @PatchMapping("/events/{eventId}/confirm")
    public ResponseEntity<CalendarEventDetailResponse> confirmEvent(
            @CurrentUser Long userId,
            @PathVariable Long eventId) {
        return ResponseEntity.ok(calendarService.confirmEvent(userId, eventId));
    }

    // PUT /api/calendar/events/{event_id}
    @PutMapping("/events/{eventId}")
    public ResponseEntity<CalendarEventDetailResponse> updateEvent(
            @CurrentUser Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody CalendarEventRequest request) {
        return ResponseEntity.ok(calendarService.updateEvent(userId, eventId, request));
    }

    // DELETE /api/calendar/events/{event_id}
    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<CalendarDeleteResponse> deleteEvent(
            @CurrentUser Long userId,
            @PathVariable Long eventId) {
        calendarService.deleteEvent(userId, eventId);
        return ResponseEntity.ok(CalendarDeleteResponse.OK);
    }
}
