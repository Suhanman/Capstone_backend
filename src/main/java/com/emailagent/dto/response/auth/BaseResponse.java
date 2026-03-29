package com.emailagent.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public abstract class BaseResponse {

    @JsonProperty("content_type")
    private final String contentType = "application/json";

    private final boolean success = true;

    @JsonProperty("result_code")
    private final int resultCode = 200;

    @JsonProperty("result_req")
    private final String resultReq = "";
}
