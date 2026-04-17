package com.emailagent.dto.response.admin.training;

import com.emailagent.domain.entity.TrainedModel;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
public class TrainedModelListResponse extends BaseResponse {

    private final List<ModelSummary> models;

    public TrainedModelListResponse(List<ModelSummary> models) {
        this.models = models;
    }

    @Getter
    @AllArgsConstructor
    public static class ModelSummary {

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

        @JsonProperty("created_at")
        private final String createdAt;

        public static ModelSummary from(TrainedModel model) {
            return new ModelSummary(
                    model.getModelId(),
                    model.getModelVersion(),
                    model.getJobId(),
                    model.getStatus().name(),
                    model.getIntentF1(),
                    model.getDomainAccuracy(),
                    model.getCreatedAt() != null ? model.getCreatedAt().format(FORMATTER) : null
            );
        }
    }
}
