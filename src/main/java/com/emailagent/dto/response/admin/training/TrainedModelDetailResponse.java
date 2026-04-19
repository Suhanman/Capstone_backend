package com.emailagent.dto.response.admin.training;

import com.emailagent.domain.entity.TrainedModel;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class TrainedModelDetailResponse extends BaseResponse {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @JsonProperty("model_id")
    private final Long modelId;

    @JsonProperty("model_version")
    private final String modelVersion;

    @JsonProperty("job_id")
    private final String jobId;

    private final String status;

    @JsonProperty("intent_f1")
    private final Double intentF1;

    @JsonProperty("domain_accuracy")
    private final Double domainAccuracy;

    @JsonProperty("metrics_json")
    private final String metricsJson;

    @JsonProperty("created_at")
    private final String createdAt;

    public TrainedModelDetailResponse(TrainedModel model) {
        this.modelId = model.getModelId();
        this.modelVersion = model.getModelVersion();
        this.jobId = model.getJobId();
        this.status = model.getStatus().name();
        this.intentF1 = model.getIntentF1();
        this.domainAccuracy = model.getDomainAccuracy();
        this.metricsJson = model.getMetricsJson();
        this.createdAt = model.getCreatedAt() != null ? model.getCreatedAt().format(FORMATTER) : null;
    }
}
