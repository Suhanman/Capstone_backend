package com.emailagent.repository;

import com.emailagent.domain.entity.EmailAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailAnalysisResultRepository extends JpaRepository<EmailAnalysisResult, Long> {
    Optional<EmailAnalysisResult> findByEmail_EmailId(Long emailId);
}
