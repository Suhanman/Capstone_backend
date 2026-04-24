package com.emailagent.dto.response.admin.training;

import com.emailagent.domain.entity.TrainingJob;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class TrainingJobDetailResponse extends BaseResponse {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @JsonProperty("job_id")
    private final String jobId;

    @JsonProperty("job_type")
    private final String jobType;

    @JsonProperty("task_type")
    private final String taskType;

    @JsonProperty("dataset_version")
    private final String datasetVersion;

    @JsonProperty("requested_by")
    private final String requestedBy;

    private final String status;

    @JsonProperty("model_version")
    private final String modelVersion;

    @JsonProperty("metrics_json")
    private final String metricsJson;

    @JsonProperty("error_message")
    private final String errorMessage;

    @JsonProperty("created_at")
    private final String createdAt;

    @JsonProperty("finished_at")
    private final String finishedAt;

    public TrainingJobDetailResponse(TrainingJob job) {
        this.jobId = job.getJobId();
        this.jobType = job.getJobType();
        this.taskType = job.getTaskType();
        this.datasetVersion = job.getDatasetVersion();
        this.requestedBy = job.getRequestedBy();
        this.status = job.getStatus().name();
        this.modelVersion = job.getModelVersion();
        this.metricsJson = job.getMetricsJson();
        this.errorMessage = job.getErrorMessage();
        this.createdAt = job.getCreatedAt() != null ? job.getCreatedAt().format(FORMATTER) : null;
        this.finishedAt = job.getFinishedAt() != null ? job.getFinishedAt().format(FORMATTER) : null;
    }
}
