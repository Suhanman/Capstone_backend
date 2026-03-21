package com.emailagent.repository;

import com.emailagent.domain.entity.Email;
import com.emailagent.domain.enums.EmailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 분석 결과 포함 조회 (N+1 방지)
    @Query("SELECT e FROM Email e " +
           "LEFT JOIN FETCH e.user " +
           "WHERE e.emailId = :emailId AND e.user.userId = :userId")
    Optional<Email> findByEmailIdAndUserId(@Param("emailId") Long emailId,
                                            @Param("userId") Long userId);
}
