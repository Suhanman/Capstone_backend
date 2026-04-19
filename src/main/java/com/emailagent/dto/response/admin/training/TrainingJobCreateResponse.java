package com.emailagent.dto.response.admin.training;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class TrainingJobCreateResponse extends BaseResponse {

    @JsonProperty("job_id")
    private final String jobId;

    private final String status;

    @JsonProperty("dataset_version")
    private final String datasetVersion;

    @JsonProperty("created_at")
    private final String createdAt;

    public TrainingJobCreateResponse(String jobId, String status, String datasetVersion, String createdAt) {
        this.jobId = jobId;
        this.status = status;
        this.datasetVersion = datasetVersion;
        this.createdAt = createdAt;
    }
}
