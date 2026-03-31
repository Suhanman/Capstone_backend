package com.emailagent.repository;

import com.emailagent.domain.entity.EmailAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailAttachmentRepository extends JpaRepository<EmailAttachment, Long> {

    /** email_id + attachment_id 조합으로 조회 (소유권 검증 포함) */
    Optional<EmailAttachment> findByAttachmentIdAndEmail_EmailId(Long attachmentId, Long emailId);
}
