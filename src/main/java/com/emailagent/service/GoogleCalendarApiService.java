package com.emailagent.service;

import com.emailagent.domain.entity.CalendarEvent;
import com.emailagent.domain.entity.Integration;
import com.emailagent.exception.CalendarNotConnectedException;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.repository.IntegrationRepository;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;

/**
 * Google Calendar 공식 Java 클라이언트를 사용해 실제 캘린더 이벤트 CRUD를 담당하는 서비스.
 *
 * 모든 API 호출 전 is_calendar_connected 검증을 수행하여
 * Granular Consent로 캘린더 권한을 거부한 사용자를 사전 차단한다.
 *
 * 호출 흐름:
 * 1) userId로 Integration(토큰 + 연동 상태) 조회
 * 2) is_calendar_connected 검증 — false면 CalendarNotConnectedException
 * 3) GoogleApiClientProvider로 Calendar 클라이언트 생성 (토큰 자동 갱신)
 * 4) Google Calendar API 호출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarApiService {

    private final IntegrationRepository integrationRepository;
    private final GoogleApiClientProvider googleApiClientProvider;

    @Value("${app.google.calendar-id}")
    private String calendarId;

    /**
     * Google Calendar에 새 일정을 등록하고 생성된 Google 이벤트 ID를 반환한다.
     *
     * @return Google Calendar가 발급한 이벤트 ID (이후 update/delete에 사용)
     * @throws CalendarNotConnectedException 캘린더 권한 미동의 시
     */
    public String createEvent(Long userId, CalendarEvent event) {
        Calendar calendarClient = buildCalendarClient(userId);
        try {
            Event googleEvent = toGoogleEvent(event);
            Event created = calendarClient.events().insert(calendarId, googleEvent).execute();

            log.info("[Calendar] 일정 생성 완료 — userId={}, googleEventId={}, title={}",
                    userId, created.getId(), event.getTitle());
            return created.getId();

        } catch (Exception e) {
            log.error("[Calendar] 일정 생성 실패 — userId={}, title={}, error={}",
                    userId, event.getTitle(), e.getMessage(), e);
            throw new RuntimeException("Google Calendar 일정 생성에 실패했습니다.", e);
        }
    }

    /**
     * Google Calendar의 기존 일정을 수정한다.
     *
     * @param googleEventId Google Calendar 이벤트 ID
     * @throws CalendarNotConnectedException 캘린더 권한 미동의 시
     */
    public void updateEvent(Long userId, String googleEventId, CalendarEvent event) {
        Calendar calendarClient = buildCalendarClient(userId);
        try {
            Event googleEvent = toGoogleEvent(event);
            calendarClient.events().update(calendarId, googleEventId, googleEvent).execute();

            log.info("[Calendar] 일정 수정 완료 — userId={}, googleEventId={}, title={}",
                    userId, googleEventId, event.getTitle());

        } catch (Exception e) {
            log.error("[Calendar] 일정 수정 실패 — userId={}, googleEventId={}, error={}",
                    userId, googleEventId, e.getMessage(), e);
            throw new RuntimeException("Google Calendar 일정 수정에 실패했습니다.", e);
        }
    }

    /**
     * Google Calendar에서 일정을 삭제한다.
     *
     * @param googleEventId Google Calendar 이벤트 ID
     * @throws CalendarNotConnectedException 캘린더 권한 미동의 시
     */
    public void deleteEvent(Long userId, String googleEventId) {
        Calendar calendarClient = buildCalendarClient(userId);
        try {
            calendarClient.events().delete(calendarId, googleEventId).execute();

            log.info("[Calendar] 일정 삭제 완료 — userId={}, googleEventId={}", userId, googleEventId);

        } catch (Exception e) {
            log.error("[Calendar] 일정 삭제 실패 — userId={}, googleEventId={}, error={}",
                    userId, googleEventId, e.getMessage(), e);
            throw new RuntimeException("Google Calendar 일정 삭제에 실패했습니다.", e);
        }
    }

    // ── private 헬퍼 ────────────────────────────────────────────────────────────

    /**
     * Integration 조회 → is_calendar_connected 검증 → Calendar 클라이언트 생성.
     * 모든 public 메서드의 진입점에서 공통으로 호출하여 권한 검증을 보장한다.
     */
    private Calendar buildCalendarClient(Long userId) {
        Integration integration = integrationRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Google 연동 정보를 찾을 수 없습니다. userId=" + userId));

        // Granular Consent: 사용자가 캘린더 권한을 거부했을 경우 차단
        if (!integration.isCalendarConnected()) {
            throw new CalendarNotConnectedException();
        }

        try {
            return googleApiClientProvider.buildCalendarClient(integration);
        } catch (Exception e) {
            log.error("[Calendar] Calendar 클라이언트 생성 실패 — userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("Google Calendar 클라이언트 생성에 실패했습니다.", e);
        }
    }

    /**
     * CalendarEvent 엔티티를 Google Calendar API의 Event 객체로 변환한다.
     * - start/end: LocalDateTime → UTC 기반 DateTime (timeZone: Asia/Seoul 명시)
     * - end가 null인 경우 start + 1시간으로 기본값 처리
     */
    private Event toGoogleEvent(CalendarEvent event) {
        DateTime startDt = new DateTime(
                event.getStartDatetime().toInstant(ZoneOffset.UTC).toEpochMilli());

        // endDatetime이 없으면 시작 시각 + 1시간으로 기본 처리
        DateTime endDt = new DateTime(
                (event.getEndDatetime() != null
                        ? event.getEndDatetime()
                        : event.getStartDatetime().plusHours(1))
                        .toInstant(ZoneOffset.UTC).toEpochMilli());

        EventDateTime start = new EventDateTime().setDateTime(startDt).setTimeZone("Asia/Seoul");
        EventDateTime end   = new EventDateTime().setDateTime(endDt).setTimeZone("Asia/Seoul");

        return new Event()
                .setSummary(event.getTitle())
                .setStart(start)
                .setEnd(end);
    }
}
