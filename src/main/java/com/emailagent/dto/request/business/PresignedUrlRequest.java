package com.emailagent.dto.request.business;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PresignedUrlRequest {

    @NotBlank
    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("content_type")
    private String contentType;
}
