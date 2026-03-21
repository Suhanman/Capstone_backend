package com.emailagent.repository;

import com.emailagent.domain.entity.Template;
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
}
