package com.emailagent.dto.response.admin.training;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class DatasetCollectionCreateResponse extends BaseResponse {

    @JsonProperty("dataset_id")
    private final Long datasetId;

    @JsonProperty("dataset_version")
    private final String datasetVersion;

    @JsonProperty("record_count")
    private final int recordCount;

    private final String status;

    @JsonProperty("created_at")
    private final String createdAt;

    public DatasetCollectionCreateResponse(Long datasetId, String datasetVersion,
                                           int recordCount, String status, String createdAt) {
        this.datasetId = datasetId;
        this.datasetVersion = datasetVersion;
        this.recordCount = recordCount;
        this.status = status;
        this.createdAt = createdAt;
    }
}
