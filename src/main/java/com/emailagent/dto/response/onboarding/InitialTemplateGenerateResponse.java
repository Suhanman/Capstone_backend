package com.emailagent.dto.response.onboarding;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InitialTemplateGenerateResponse extends BaseResponse {

    private String status;

    @JsonProperty("processing_count")
    private int processingCount;

    public static InitialTemplateGenerateResponse of(int processingCount) {
        return InitialTemplateGenerateResponse.builder()
                .status("PROCESSING")
                .processingCount(processingCount)
                .build();
    }
}