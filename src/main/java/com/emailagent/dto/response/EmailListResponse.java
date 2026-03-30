package com.emailagent.dto.response;

import com.emailagent.domain.entity.Email;
import com.emailagent.domain.enums.EmailStatus;
import com.emailagent.domain.enums.ImportanceLevel;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EmailListResponse {

    @JsonProperty("email_id")
    private Long emailId;

    @JsonProperty("sender_name")
    private String senderName;

    @JsonProperty("sender_email")
    private String senderEmail;

    private String subject;
    private EmailStatus status;

    @JsonProperty("importance_level")
    private ImportanceLevel importanceLevel;

    @JsonProperty("received_at")
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
