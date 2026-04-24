package com.emailagent.dto.response.inbox;

import com.emailagent.domain.entity.EmailTemplateRecommendation;
import com.emailagent.domain.entity.Template;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class InboxRecommendationsResponse {

    @JsonProperty("drafts")
    private List<RecommendationItem> drafts;

    @Getter
    @Builder
    public static class RecommendationItem {
        @JsonProperty("draft_id")
        private Long draftId;

        @JsonProperty("template_title")
        private String templateTitle;

        @JsonProperty("subject")
        private String subject;

        @JsonProperty("body")
        private String body;

        @JsonProperty("similarity")
        private Double similarity;

        @JsonProperty("email_id")
        private Long emailId;

        public static RecommendationItem from(EmailTemplateRecommendation recommendation) {
            Template template = recommendation.getTemplate();
            return RecommendationItem.builder()
                    .draftId(recommendation.getRecommendationId())
                    .templateTitle(template.getTitle())
                    .subject(template.getSubjectTemplate())
                    .body(template.getBodyTemplate())
                    .similarity(recommendation.getScore())
                    .emailId(recommendation.getEmail().getEmailId())
                    .build();
        }
    }
}
