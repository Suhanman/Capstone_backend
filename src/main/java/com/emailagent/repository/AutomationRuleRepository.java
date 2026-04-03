package com.emailagent.repository;

import com.emailagent.domain.entity.AutomationRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AutomationRuleRepository extends JpaRepository<AutomationRule, Long> {

    // N+1 방지: category, template FETCH JOIN
    @Query("""
            SELECT ar FROM AutomationRule ar
            JOIN FETCH ar.category
            LEFT JOIN FETCH ar.template
            WHERE ar.user.userId = :userId
            ORDER BY ar.createdAt DESC
            """)
    List<AutomationRule> findByUserIdWithDetails(@Param("userId") Long userId);

    Optional<AutomationRule> findByRuleIdAndUser_UserId(Long ruleId, Long userId);

    // 관리자 - 전체 목록 (페이징)
    @Query("""
            SELECT ar FROM AutomationRule ar
            ORDER BY ar.createdAt DESC
            """)
    Page<AutomationRule> findAllRules(Pageable pageable);

    // 관리자 - 특정 사용자 필터 (페이징)
    @Query("""
            SELECT ar FROM AutomationRule ar
            WHERE ar.user.userId = :userId
            ORDER BY ar.createdAt DESC
            """)
    Page<AutomationRule> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // 관리자 - 상세 조회
    @Query("""
            SELECT ar FROM AutomationRule ar
            JOIN FETCH ar.category
            LEFT JOIN FETCH ar.template
            WHERE ar.ruleId = :ruleId
            """)
    Optional<AutomationRule> findDetailByRuleId(@Param("ruleId") Long ruleId);
}
