package com.emailagent.dto.response.admin.training;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class DeploymentJobResponse extends BaseResponse {

    @JsonProperty("job_id")
    private final String jobId;

    private final String status;

    @JsonProperty("job_type")
    private final String jobType;

    @JsonProperty("created_at")
    private final String createdAt;

    public DeploymentJobResponse(String jobId, String status, String jobType, String createdAt) {
        this.jobId = jobId;
        this.status = status;
        this.jobType = jobType;
        this.createdAt = createdAt;
    }
}