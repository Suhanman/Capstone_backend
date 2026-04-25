package com.emailagent.dto.response.business;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresignedUrlResponse extends BaseResponse {

    @JsonProperty("presigned_url")
    private String presignedUrl;

    @JsonProperty("s3_key")
    private String s3Key;
}
