package com.emailagent.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG template index 결과 DTO.
 */
@Getter
@NoArgsConstructor
public class RagTemplateIndexResultDTO {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("payload")
    private Payload payload;

    @JsonProperty("error")
    private ErrorPayload error;

    @Getter
    @NoArgsConstructor
    public static class Payload {
        @JsonProperty("indexed_template_count")
        private Integer indexedTemplateCount;

        @JsonProperty("template_ids")
        private List<Long> templateIds;
    }

    @Getter
    @NoArgsConstructor
    public static class ErrorPayload {
        @JsonProperty("code")
        private String code;

        @JsonProperty("message")
        private String message;
    }
}
