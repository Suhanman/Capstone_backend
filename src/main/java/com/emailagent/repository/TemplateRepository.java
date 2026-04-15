package com.emailagent.repository;

import com.emailagent.domain.entity.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    List<Template> findByUser_UserId(Long userId);

    List<Template> findByCategory_CategoryId(Long categoryId);

    @Query("SELECT t FROM Template t WHERE t.user.userId = :userId " +
           "AND t.category.categoryId = :categoryId ORDER BY t.accuracyScore DESC")
    Optional<Template> findBestMatchingTemplate(@Param("userId") Long userId,
                                                @Param("categoryId") Long categoryId);

    Optional<Template> findByUser_UserIdAndCategory_CategoryIdAndVariantLabel(Long userId, Long categoryId, String variantLabel);

    // 관리자 - 전체 템플릿 페이징 (user FETCH JOIN으로 N+1 방지)
    @Query(value = "SELECT t FROM Template t JOIN FETCH t.user ORDER BY t.createdAt DESC",
           countQuery = "SELECT COUNT(t) FROM Template t")
    Page<Template> findAllWithUserOrderByCreatedAtDesc(Pageable pageable);

    // 관리자 - 특정 사용자 템플릿 페이징 (user FETCH JOIN)
    @Query(value = "SELECT t FROM Template t JOIN FETCH t.user WHERE t.user.userId = :userId ORDER BY t.createdAt DESC",
           countQuery = "SELECT COUNT(t) FROM Template t WHERE t.user.userId = :userId")
    Page<Template> findByUserIdWithUserOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    // 관리자 - 카테고리별 템플릿 수 + 누적 사용 횟수 통계 (native)
    // TemplateUsageLogs 엔티티가 없으므로 native SQL로 직접 집계
    @Query(value = """
            SELECT c.category_id,
                   c.category_name,
                   COUNT(DISTINCT t.template_id) AS template_count,
                   COUNT(tul.usage_log_id)        AS usage_count
            FROM Categories c
            LEFT JOIN Templates t   ON c.category_id = t.category_id
            LEFT JOIN TemplateUsageLogs tul ON t.template_id = tul.template_id
            GROUP BY c.category_id, c.category_name
            ORDER BY template_count DESC
            """, nativeQuery = true)
    List<Object[]> findCategoryStatistics();
}
