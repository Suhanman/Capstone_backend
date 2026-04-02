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

    long countByUser_UserId(Long userId);

    // 관리자 대시보드: 오늘 초안 생성 건수
    @Query("SELECT COUNT(d) FROM DraftReply d WHERE d.createdAt >= :start AND d.createdAt < :end")
    long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 관리자 대시보드 - 주간 추이: 날짜별 초안 생성 수 (native)
    @Query(value = "SELECT DATE(created_at) as date, COUNT(*) as count FROM DraftReplies WHERE created_at >= :start GROUP BY DATE(created_at) ORDER BY DATE(created_at)",
           nativeQuery = true)
    List<Object[]> countDraftsGroupedByDate(@Param("start") LocalDateTime start);

    /**
     * reply_embedding 기준 코사인 유사도 상위 topK 초안 조회 (MariaDB VEC_DISTANCE_COSINE)
     * embedding: 비교 대상 이메일의 email_embedding (byte[])
     * similarity 오름차순(거리 기준) = 유사한 것이 먼저 나옴
     */
    @Query(value = """
            SELECT d.draft_reply_id, d.draft_subject, d.draft_content,
                   VEC_DISTANCE_COSINE(d.reply_embedding, :embedding) AS similarity,
                   d.email_id
            FROM DraftReplies d
            WHERE d.reply_embedding IS NOT NULL
            ORDER BY similarity ASC
            LIMIT :topK
            """, nativeQuery = true)
    List<Object[]> findTopKSimilarDrafts(@Param("embedding") byte[] embedding,
                                          @Param("topK") int topK);
}
