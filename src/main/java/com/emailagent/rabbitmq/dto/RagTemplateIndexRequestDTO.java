package com.emailagent.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 저장된 Template를 RAG template index에 적재하기 위한 요청 DTO.
 */
@Getter
@Builder
public class RagTemplateIndexRequestDTO {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("payload")
    private Payload payload;

    @Getter
    @Builder
    public static class Payload {
        @JsonProperty("templates")
        private List<TemplateItem> templates;
    }

    @Getter
    @Builder
    public static class TemplateItem {
        @JsonProperty("template_id")
        private Long templateId;

        @JsonProperty("title")
        private String title;

        @JsonProperty("category_name")
        private String categoryName;

        @JsonProperty("email_tone")
        private String emailTone;
    }
}
