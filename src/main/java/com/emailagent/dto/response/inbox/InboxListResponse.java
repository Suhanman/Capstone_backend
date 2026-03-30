package com.emailagent.dto.response.inbox;

import com.emailagent.domain.entity.Email;
import com.emailagent.domain.entity.EmailAnalysisResult;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class InboxListResponse extends BaseResponse {

    @JsonProperty("total_elements")
    private long totalElements;

    private List<EmailItem> content;

    @Getter
    @Builder
    public static class EmailItem {
        @JsonProperty("email_id")
        private Long emailId;

        @JsonProperty("sender_name")
        private String senderName;

        private String subject;

        @JsonProperty("received_at")
        private LocalDateTime receivedAt;

        private String status;

        @JsonProperty("category_name")
        private String categoryName;

        @JsonProperty("schedule_detected")
        private boolean scheduleDetected;

        public static EmailItem from(Email email) {
            EmailAnalysisResult ar = email.getAnalysisResult();
            return EmailItem.builder()
                    .emailId(email.getEmailId())
                    .senderName(email.getSenderName())
                    .subject(email.getSubject())
                    .receivedAt(email.getReceivedAt())
                    .status(email.getStatus().name())
                    .categoryName(ar != null && ar.getCategory() != null
                            ? ar.getCategory().getCategoryName() : null)
                    .scheduleDetected(ar != null && ar.isScheduleDetected())
                    .build();
        }
    }
}
