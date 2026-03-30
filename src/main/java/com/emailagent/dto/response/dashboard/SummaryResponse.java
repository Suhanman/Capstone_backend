package com.emailagent.dto.response.dashboard;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SummaryResponse extends BaseResponse {

    @JsonProperty("processed_today")
    private ProcessedToday processedToday;

    @JsonProperty("pending_drafts")
    private PendingDrafts pendingDrafts;

    @JsonProperty("template_matching")
    private TemplateMatching templateMatching;

    @JsonProperty("integration_status")
    private IntegrationStatus integrationStatus;

    @Getter
    @Builder
    public static class ProcessedToday {
        private long count;

        @JsonProperty("diff_from_yesterday")
        private long diffFromYesterday;
    }

    @Getter
    @Builder
    public static class PendingDrafts {
        private long count;
    }

    @Getter
    @Builder
    public static class TemplateMatching {
        private double rate;

        @JsonProperty("diff_from_last_week")
        private double diffFromLastWeek;
    }

    @Getter
    @Builder
    public static class IntegrationStatus {
        private String status;

        @JsonProperty("connected_email")
        private String connectedEmail;
    }
}
