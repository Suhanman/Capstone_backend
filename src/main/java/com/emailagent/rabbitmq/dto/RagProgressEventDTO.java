package com.emailagent.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * RAG 진행 상태 이벤트 DTO.
 */
@Getter
@NoArgsConstructor
public class RagProgressEventDTO {

    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("job_type")
    private String jobType;

    @JsonProperty("target_type")
    private String targetType;

    @JsonProperty("target_id")
    private String targetId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("progress_step")
    private String progressStep;

    @JsonProperty("progress_message")
    private String progressMessage;

    @JsonProperty("payload")
    private Map<String, Object> payload;

    @JsonProperty("error")
    private ErrorPayload error;

    @Getter
    @NoArgsConstructor
    public static class ErrorPayload {
        @JsonProperty("code")
        private String code;

        @JsonProperty("message")
        private String message;
    }
}
