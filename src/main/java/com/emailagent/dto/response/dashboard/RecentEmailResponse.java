package com.emailagent.dto.response.dashboard;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RecentEmailResponse extends BaseResponse {

    private List<EmailItem> emails;

    @Getter
    @Builder
    public static class EmailItem {

        @JsonProperty("email_id")
        private Long emailId;

        @JsonProperty("sender_name")
        private String senderName;

        @JsonProperty("sender_company")
        private String senderCompany;

        private String subject;

        @JsonProperty("category_name")
        private String categoryName;

        private String status;

        @JsonProperty("received_at")
        private LocalDateTime receivedAt;
    }
}
