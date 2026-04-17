package com.emailagent.dto.request.admin.training;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TrainingJobCreateRequest {

    @NotBlank(message = "dataset_version은 필수입니다.")
    @JsonProperty("dataset_version")
    private String datasetVersion;
}
