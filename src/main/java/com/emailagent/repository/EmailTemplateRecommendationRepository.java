package com.emailagent.repository;

import com.emailagent.domain.entity.EmailTemplateRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmailTemplateRecommendationRepository extends JpaRepository<EmailTemplateRecommendation, Long> {

    @Query("""
            SELECT r FROM EmailTemplateRecommendation r
            JOIN FETCH r.template t
            WHERE r.user.userId = :userId
              AND r.email.emailId = :emailId
            ORDER BY r.rankOrder ASC, r.score DESC
            """)
    List<EmailTemplateRecommendation> findByUserIdAndEmailIdOrderByRank(
            @Param("userId") Long userId,
            @Param("emailId") Long emailId
    );

    void deleteByUser_UserIdAndEmail_EmailId(Long userId, Long emailId);

    Optional<EmailTemplateRecommendation> findByEmail_EmailIdAndTemplate_TemplateId(Long emailId, Long templateId);
}
