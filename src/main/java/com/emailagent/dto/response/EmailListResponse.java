package com.emailagent.dto.response;

import com.emailagent.domain.entity.Email;
import com.emailagent.domain.enums.EmailStatus;
import com.emailagent.domain.enums.ImportanceLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EmailListResponse {
    private Long emailId;
    private String senderName;
    private String senderEmail;
    private String subject;
    private EmailStatus status;
    private ImportanceLevel importanceLevel;
    private LocalDateTime receivedAt;

    public static EmailListResponse from(Email email) {
        return EmailListResponse.builder()
                .emailId(email.getEmailId())
                .senderName(email.getSenderName())
                .senderEmail(email.getSenderEmail())
                .subject(email.getSubject())
                .status(email.getStatus())
                .importanceLevel(email.getImportanceLevel())
                .receivedAt(email.getReceivedAt())
                .build();
    }
}
