package com.emailagent.dto.response.admin.training;

import com.emailagent.domain.entity.TrainingDataset;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
public class DatasetListResponse extends BaseResponse {

    private final List<DatasetSummary> datasets;

    public DatasetListResponse(List<DatasetSummary> datasets) {
        this.datasets = datasets;
    }

    @Getter
    @AllArgsConstructor
    public static class DatasetSummary {

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @JsonProperty("dataset_id")
        private final Long datasetId;

        @JsonProperty("dataset_version")
        private final String datasetVersion;

        @JsonProperty("record_count")
        private final int recordCount;

        private final String status;

        @JsonProperty("created_at")
        private final String createdAt;

        public static DatasetSummary from(TrainingDataset dataset) {
            return new DatasetSummary(
                    dataset.getDatasetId(),
                    dataset.getDatasetVersion(),
                    dataset.getRecordCount(),
                    dataset.getStatus().name(),
                    dataset.getCreatedAt() != null ? dataset.getCreatedAt().format(FORMATTER) : null
            );
        }
    }
}
