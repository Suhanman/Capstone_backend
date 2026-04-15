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

    private final List<DraftItem> drafts;

    public RecommendedDraftResponse(List<DraftItem> drafts) {
        super();
        this.drafts = drafts;
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

        @JsonProperty("template_title")
        private final String templateTitle;

        public DraftItem(Long draftId, String subject, String body, double similarity, Long emailId, String templateTitle) {
            this.draftId = draftId;
            this.subject = subject;
            this.body = body;
            this.similarity = similarity;
            this.emailId = emailId;
            this.templateTitle = templateTitle;
        }
    }
}
