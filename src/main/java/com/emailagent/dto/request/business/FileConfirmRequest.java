package com.emailagent.dto.request.business;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class FileConfirmRequest {

    @NotBlank
    @JsonProperty("s3_key")
    private String s3Key;

    @NotBlank
    @JsonProperty("file_name")
    private String fileName;
}
