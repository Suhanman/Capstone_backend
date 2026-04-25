package com.emailagent.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 템플릿 매칭 결과 DTO.
 */
@Getter
@NoArgsConstructor
public class RagTemplateMatchResultDTO {

    @JsonProperty("job_id")
    private String jobId;

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
        @JsonProperty("emailId")
        private String emailId;

        @JsonProperty("results")
        private List<ResultItem> results;
    }

    @Getter
    @NoArgsConstructor
    public static class ResultItem {
        @JsonProperty("template_id")
        private Long templateId;

        @JsonProperty("title")
        private String title;

        @JsonProperty("intent")
        private String intent;

        @JsonProperty("score")
        private Double score;
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
