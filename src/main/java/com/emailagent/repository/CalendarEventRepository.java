package com.emailagent.repository;

import com.emailagent.domain.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    // 오늘 이후 일정 (대시보드용)
    @Query("""
            SELECT ce FROM CalendarEvent ce
            LEFT JOIN FETCH ce.email
            WHERE ce.user.userId = :userId
              AND ce.startDatetime >= :from
            ORDER BY ce.startDatetime ASC
            """)
    List<CalendarEvent> findUpcoming(@Param("userId") Long userId,
                                     @Param("from") LocalDateTime from);

    // 이메일에 연결된 미처리 일정 (PENDING 상태만)
    Optional<CalendarEvent> findByEmail_EmailIdAndUser_UserIdAndStatus(
            Long emailId, Long userId, String status);

    // 이메일 상세 조회 시 일정 정보 표시용 (상태 무관)
    Optional<CalendarEvent> findByEmail_EmailIdAndUser_UserId(Long emailId, Long userId);

    // 기간 내 일정 목록 (캘린더 API용)
    @Query("""
            SELECT ce FROM CalendarEvent ce
            LEFT JOIN FETCH ce.email
            WHERE ce.user.userId = :userId
              AND ce.startDatetime >= :startDate
              AND ce.startDatetime <= :endDate
            ORDER BY ce.startDatetime ASC
            """)
    List<CalendarEvent> findByPeriod(@Param("userId") Long userId,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);
}
