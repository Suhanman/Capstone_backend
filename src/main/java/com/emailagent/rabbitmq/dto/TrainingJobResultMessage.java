package com.emailagent.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AI Training Worker → Backend 완료 이벤트 메시지 DTO.
 * queue: q.2app.training (AI가 x.ai2app.direct를 통해 발행)
 * status: "completed" 또는 "failed"
 */
@Getter
@NoArgsConstructor
public class TrainingJobResultMessage {

    @JsonProperty("job_id")
    private String jobId;

    private String status;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("finished_at")
    private String finishedAt;

    private Map<String, Object> metrics;

    @JsonProperty("error_message")
    private String errorMessage;
}
