package com.emailagent.repository;

import com.emailagent.domain.entity.Email;
import com.emailagent.domain.enums.EmailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailRepository extends JpaRepository<Email, Long> {

    // 사용자의 이메일 목록 (페이징)
    Page<Email> findByUser_UserIdOrderByReceivedAtDesc(Long userId, Pageable pageable);

    // 상태별 조회
    Page<Email> findByUser_UserIdAndStatusOrderByReceivedAtDesc(
            Long userId, EmailStatus status, Pageable pageable);

    // Gmail 메시지 ID로 중복 체크
    boolean existsByExternalMsgId(String externalMsgId);

    Optional<Email> findByExternalMsgId(String externalMsgId);

    @Query("SELECT e.user.userId FROM Email e WHERE e.emailId = :emailId")
    Optional<Long> findUserIdByEmailId(@Param("emailId") Long emailId);

    // 분석 결과 포함 조회 (N+1 방지)
    @Query("SELECT e FROM Email e " +
           "LEFT JOIN FETCH e.user " +
           "WHERE e.emailId = :emailId AND e.user.userId = :userId")
    Optional<Email> findByEmailIdAndUserId(@Param("emailId") Long emailId,
                                            @Param("userId") Long userId);

    // 특정 기간 이메일 수 카운트 (대시보드용) — DELETED 제외
    @Query("SELECT COUNT(e) FROM Email e WHERE e.user.userId = :userId AND e.receivedAt >= :start AND e.receivedAt < :end AND e.status != com.emailagent.domain.enums.EmailStatus.DELETED")
    long countByUserIdAndDateRange(@Param("userId") Long userId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    // 최근 이메일 N건 (대시보드용, N+1 방지) — DELETED 제외
    @Query("SELECT e FROM Email e WHERE e.user.userId = :userId AND e.status != com.emailagent.domain.enums.EmailStatus.DELETED ORDER BY e.receivedAt DESC")
    List<Email> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    // 수신함 목록 — 분석결과+카테고리 FETCH JOIN, 별도 countQuery로 페이징 정확성 보장, DELETED 제외
    @Query(value = """
            SELECT e FROM Email e
            LEFT JOIN FETCH e.analysisResult ar
            LEFT JOIN FETCH ar.category
            WHERE e.user.userId = :userId
            AND e.status != com.emailagent.domain.enums.EmailStatus.DELETED
            ORDER BY e.receivedAt DESC
            """,
           countQuery = "SELECT COUNT(e) FROM Email e WHERE e.user.userId = :userId AND e.status != com.emailagent.domain.enums.EmailStatus.DELETED")
    Page<Email> findInboxPage(@Param("userId") Long userId, Pageable pageable);

    @Query(value = """
            SELECT e FROM Email e
            LEFT JOIN FETCH e.analysisResult ar
            LEFT JOIN FETCH ar.category
            WHERE e.user.userId = :userId AND e.status = :status
            ORDER BY e.receivedAt DESC
            """,
           countQuery = "SELECT COUNT(e) FROM Email e WHERE e.user.userId = :userId AND e.status = :status")
    Page<Email> findInboxPageByStatus(@Param("userId") Long userId,
                                      @Param("status") EmailStatus status,
                                      Pageable pageable);

    // 상세 조회 — 분석결과+카테고리 FETCH JOIN
    @Query("""
            SELECT e FROM Email e
            LEFT JOIN FETCH e.analysisResult ar
            LEFT JOIN FETCH ar.category
            WHERE e.emailId = :emailId AND e.user.userId = :userId
            """)
    Optional<Email> findDetailByEmailIdAndUserId(@Param("emailId") Long emailId,
                                                  @Param("userId") Long userId);

    // 관리자 - 사용자 누적 처리 이메일 수 (PENDING_REVIEW 제외)
    @Query("SELECT COUNT(e) FROM Email e WHERE e.user.userId = :userId AND e.status != com.emailagent.domain.enums.EmailStatus.PENDING_REVIEW")
    long countProcessedByUserId(@Param("userId") Long userId);

    // 관리자 대시보드 - 기간별 날짜별 메일 처리량 (native)
    @Query(value = "SELECT DATE(received_at) as date, COUNT(*) as count FROM Emails WHERE received_at >= :start AND received_at < :end GROUP BY DATE(received_at) ORDER BY DATE(received_at)",
           nativeQuery = true)
    List<Object[]> countByDateRangeGroupedByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 관리자 대시보드 - 도메인별 이메일 분포 (native, SUBSTRING_INDEX)
    @Query(value = "SELECT SUBSTRING_INDEX(sender_email, '@', -1) as domain, COUNT(*) as count FROM Emails GROUP BY domain ORDER BY count DESC LIMIT :lim",
           nativeQuery = true)
    List<Object[]> findTopDomains(@Param("lim") int lim);

    // 관리자 대시보드 - 주간 추이: 날짜별 수신 수 (native)
    @Query(value = "SELECT DATE(received_at) as date, COUNT(*) as count FROM Emails WHERE received_at >= :start GROUP BY DATE(received_at) ORDER BY DATE(received_at)",
           nativeQuery = true)
    List<Object[]> countReceivedGroupedByDate(@Param("start") LocalDateTime start);
}
