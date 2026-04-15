package com.emailagent.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * RAG 온보딩 템플릿 생성 결과를 역직렬화하는 DTO.
 */
@Getter
@NoArgsConstructor
public class RagDraftGenerateResultDTO {

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
        @JsonProperty("category_id")
        private Long categoryId;

        @JsonProperty("category_name")
        private String categoryName;

        @JsonProperty("title")
        private String title;

        @JsonProperty("subject_template")
        private String subjectTemplate;

        @JsonProperty("body_template")
        private String bodyTemplate;
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
