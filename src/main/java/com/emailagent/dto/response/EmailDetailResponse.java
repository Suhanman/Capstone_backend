package com.emailagent.dto.response;

import com.emailagent.domain.entity.Email;
import com.emailagent.domain.enums.EmailStatus;
import com.emailagent.domain.enums.ImportanceLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EmailDetailResponse {
    private Long emailId;
    private String senderName;
    private String senderEmail;
    private String subject;
    private String bodyClean;
    private EmailStatus status;
    private ImportanceLevel importanceLevel;
    private LocalDateTime receivedAt;

    public static EmailDetailResponse from(Email email) {
        return EmailDetailResponse.builder()
                .emailId(email.getEmailId())
                .senderName(email.getSenderName())
                .senderEmail(email.getSenderEmail())
                .subject(email.getSubject())
                .bodyClean(email.getBodyClean())
                .status(email.getStatus())
                .importanceLevel(email.getImportanceLevel())
                .receivedAt(email.getReceivedAt())
                .build();
    }
}
