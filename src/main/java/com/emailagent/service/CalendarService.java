package com.emailagent.service;

import com.emailagent.domain.entity.CalendarEvent;
import com.emailagent.domain.entity.User;
import com.emailagent.dto.request.calendar.CalendarEventRequest;
import com.emailagent.dto.response.calendar.CalendarEventDetailResponse;
import com.emailagent.dto.response.calendar.CalendarEventListResponse;
import com.emailagent.dto.response.calendar.CalendarEventResponse;
import com.emailagent.exception.CalendarNotConnectedException;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.repository.CalendarEventRepository;
import com.emailagent.repository.IntegrationRepository;
import com.emailagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarEventRepository calendarEventRepository;
    private final UserRepository userRepository;
    private final IntegrationRepository integrationRepository;
    private final GoogleCalendarApiService googleCalendarApiService;

    // =============================================
    // GET /api/calendar/events?start_date=&end_date=
    // 기간 내 일정 목록 조회
    // =============================================
    @Transactional(readOnly = true)
    public CalendarEventListResponse getEvents(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return CalendarEventListResponse.builder()
                .data(calendarEventRepository.findByPeriod(userId, startDate, endDate)
                        .stream()
                        .map(CalendarEventResponse::from)
                        .toList())
                .build();
    }

    // =============================================
    // GET /api/calendar/events/{event_id}
    // 일정 상세 조회
    // =============================================
    @Transactional(readOnly = true)
    public CalendarEventDetailResponse getEvent(Long userId, Long eventId) {
        CalendarEvent event = findEventForUser(eventId, userId);
        return CalendarEventDetailResponse.from(event);
    }

    // =============================================
    // POST /api/calendar/events
    // 수동 일정 추가 (source=MANUAL, status=CONFIRMED, is_calendar_added=true)
    // =============================================
    @Transactional
    public CalendarEventDetailResponse createEvent(Long userId, CalendarEventRequest request) {
        // 캘린더 연동 여부 검증 — is_calendar_connected=false 이면 비즈니스 예외
        integrationRepository.findByUser_UserId(userId)
                .filter(i -> i.isCalendarConnected())
                .orElseThrow(CalendarNotConnectedException::new);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        CalendarEvent event = CalendarEvent.builder()
                .user(user)
                .title(request.getTitle())
                .startDatetime(request.getStartDatetime())
                .endDatetime(request.getEndDatetime())
                .source("MANUAL")
                .status("CONFIRMED")
                .isCalendarAdded(true)
                .build();

        CalendarEvent saved = calendarEventRepository.save(event);

        // Google Calendar API에 일정 등록 후 반환된 googleEventId 저장
        String googleEventId = googleCalendarApiService.createEvent(userId, saved);
        saved.markAsCalendarAdded(googleEventId);

        return CalendarEventDetailResponse.from(saved);
    }

    // =============================================
    // PATCH /api/calendar/events/{event_id}/confirm
    // PENDING → CONFIRMED 상태 변경
    // =============================================
    @Transactional
    public CalendarEventDetailResponse confirmEvent(Long userId, Long eventId) {
        CalendarEvent event = findEventForUser(eventId, userId);

        if (!"PENDING".equals(event.getStatus())) {
            throw new IllegalStateException("PENDING 상태인 일정만 확정할 수 있습니다. 현재 상태: " + event.getStatus());
        }

        event.updateStatus("CONFIRMED");

        // CONFIRMED 전환 시 Google Calendar에 등록 (최초 1회)
        // GoogleCalendarApiService 내부에서 is_calendar_connected 검증 수행
        String googleEventId = googleCalendarApiService.createEvent(userId, event);
        event.markAsCalendarAdded(googleEventId);

        return CalendarEventDetailResponse.from(event);
    }

    // =============================================
    // PUT /api/calendar/events/{event_id}
    // 일정 수정
    // =============================================
    @Transactional
    public CalendarEventDetailResponse updateEvent(Long userId, Long eventId, CalendarEventRequest request) {
        CalendarEvent event = findEventForUser(eventId, userId);

        event.update(request.getTitle(), request.getStartDatetime(), request.getEndDatetime());

        // Google Calendar에 등록된 일정만 API로 수정 (googleEventId 있을 때만)
        if (event.isCalendarAdded() && event.getGoogleEventId() != null) {
            googleCalendarApiService.updateEvent(userId, event.getGoogleEventId(), event);
        }

        return CalendarEventDetailResponse.from(event);
    }

    // =============================================
    // DELETE /api/calendar/events/{event_id}
    // 일정 삭제
    // =============================================
    @Transactional
    public void deleteEvent(Long userId, Long eventId) {
        CalendarEvent event = findEventForUser(eventId, userId);

        // Google Calendar에 등록된 일정은 API로 먼저 삭제 후 DB에서 제거
        if (event.isCalendarAdded() && event.getGoogleEventId() != null) {
            googleCalendarApiService.deleteEvent(userId, event.getGoogleEventId());
        }

        calendarEventRepository.delete(event);
    }

    // =============================================
    // 내부 헬퍼: 소유권 검증 포함 단건 조회
    // =============================================
    private CalendarEvent findEventForUser(Long eventId, Long userId) {
        CalendarEvent event = calendarEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("일정을 찾을 수 없습니다."));
        if (!event.getUser().getUserId().equals(userId)) {
            throw new ResourceNotFoundException("일정을 찾을 수 없습니다.");
        }
        return event;
    }
}
