package com.emailagent.dto.response.recommend;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * GET /api/inbox/{emailId}/recommendations 응답 DTO
 * BaseResponse(Flat 구조)에 추천 초안 목록 포함
 */
@Getter
public class RecommendedDraftResponse extends BaseResponse {

    @JsonProperty("data")
    private final List<DraftItem> data;

    public RecommendedDraftResponse(List<DraftItem> data) {
        super();
        this.data = data;
    }

    @Getter
    public static class DraftItem {

        @JsonProperty("draft_id")
        private final Long draftId;

        @JsonProperty("subject")
        private final String subject;

        @JsonProperty("body")
        private final String body;

        @JsonProperty("similarity")
        private final double similarity;

        @JsonProperty("email_id")
        private final Long emailId;

        public DraftItem(Long draftId, String subject, String body, double similarity, Long emailId) {
            this.draftId = draftId;
            this.subject = subject;
            this.body = body;
            this.similarity = similarity;
            this.emailId = emailId;
        }
    }
}
