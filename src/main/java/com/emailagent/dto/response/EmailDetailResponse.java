package com.emailagent.dto.response;

import com.emailagent.domain.entity.Email;
import com.emailagent.domain.enums.EmailStatus;
import com.emailagent.domain.enums.ImportanceLevel;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EmailDetailResponse extends BaseResponse {

    @JsonProperty("email_id")
    private Long emailId;

    @JsonProperty("sender_name")
    private String senderName;

    @JsonProperty("sender_email")
    private String senderEmail;

    private String subject;

    @JsonProperty("body_clean")
    private String bodyClean;

    private EmailStatus status;

    @JsonProperty("importance_level")
    private ImportanceLevel importanceLevel;

    @JsonProperty("received_at")
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
