package com.emailagent.dto.response.admin.training;

import com.emailagent.domain.entity.TrainedModel;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class ModelActivateResponse extends BaseResponse {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @JsonProperty("model_id")
    private final Long modelId;

    @JsonProperty("model_version")
    private final String modelVersion;

    private final String status;

    @JsonProperty("activated_at")
    private final String activatedAt;

    public ModelActivateResponse(TrainedModel model) {
        this.modelId = model.getModelId();
        this.modelVersion = model.getModelVersion();
        this.status = model.getStatus().name();
        this.activatedAt = model.getCreatedAt() != null ? model.getCreatedAt().format(FORMATTER) : null;
    }
}
