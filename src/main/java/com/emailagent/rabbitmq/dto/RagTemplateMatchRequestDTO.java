package com.emailagent.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 메일 분석 결과를 기반으로 템플릿 후보 검색을 요청하는 DTO.
 */
@Getter
@Builder
public class RagTemplateMatchRequestDTO {

    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("payload")
    private Payload payload;

    @Getter
    @Builder
    public static class Payload {
        @JsonProperty("emailId")
        private String emailId;

        @JsonProperty("subject")
        private String subject;

        @JsonProperty("body")
        private String body;

        @JsonProperty("summary")
        private String summary;

        @JsonProperty("intent")
        private String intent;

        @JsonProperty("domain")
        private String domain;

        @JsonProperty("top_k")
        private Integer topK;
    }
}
