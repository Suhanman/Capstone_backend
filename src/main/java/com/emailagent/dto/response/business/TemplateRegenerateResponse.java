package com.emailagent.dto.response.business;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TemplateRegenerateResponse extends BaseResponse {

    private String status;

    @JsonProperty("processing_count")
    private int processingCount;

    public static TemplateRegenerateResponse of(int processingCount) {
        return TemplateRegenerateResponse.builder()
                .status("PROCESSING")
                .processingCount(processingCount)
                .build();
    }
}
