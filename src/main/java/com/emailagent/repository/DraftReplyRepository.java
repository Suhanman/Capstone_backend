package com.emailagent.repository;

import com.emailagent.domain.entity.DraftReply;
import com.emailagent.domain.enums.DraftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DraftReplyRepository extends JpaRepository<DraftReply, Long> {

    long countByUser_UserIdAndStatus(Long userId, DraftStatus status);

    // template FETCH JOIN (N+1 방지)
    @Query("""
            SELECT d FROM DraftReply d
            LEFT JOIN FETCH d.template
            WHERE d.email.emailId = :emailId AND d.user.userId = :userId
            """)
    Optional<DraftReply> findByEmailIdAndUserId(@Param("emailId") Long emailId,
                                                 @Param("userId") Long userId);

    // 관리자 - 사용자 누적 초안 생성 수
    // draft 큐 Consumer에서 emailId만으로 초안 조회 (upsert 처리용)
    Optional<DraftReply> findByEmail_EmailId(Long emailId);

    // 수신함 목록 조회 시 N+1 방지용 batch 조회
    @Query("SELECT d FROM DraftReply d WHERE d.email.emailId IN :emailIds AND d.user.userId = :userId")
    List<DraftReply> findByEmailIdsAndUserId(@Param("emailIds") List<Long> emailIds,
                                             @Param("userId") Long userId);

    long countByUser_UserId(Long userId);

    // 관리자 대시보드: 오늘 초안 생성 건수
    @Query("SELECT COUNT(d) FROM DraftReply d WHERE d.createdAt >= :start AND d.createdAt < :end")
    long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 관리자 대시보드 - 주간 추이: 날짜별 초안 생성 수 (native)
    @Query(value = "SELECT DATE(created_at) as date, COUNT(*) as count FROM DraftReplies WHERE created_at >= :start GROUP BY DATE(created_at) ORDER BY DATE(created_at)",
           nativeQuery = true)
    List<Object[]> countDraftsGroupedByDate(@Param("start") LocalDateTime start);

}
