package com.emailagent.dto.response.onboarding;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class InitialTemplateGenerateResponse extends BaseResponse {

    private String status;

    @JsonProperty("processing_count")
    private int processingCount;

    @JsonProperty("job_ids")
    private List<String> jobIds;

    @JsonProperty("knowledge_job_id")
    private String knowledgeJobId;

    public static InitialTemplateGenerateResponse of(int processingCount, List<String> jobIds, String knowledgeJobId) {
        return InitialTemplateGenerateResponse.builder()
                .status("PROCESSING")
                .processingCount(processingCount)
                .jobIds(jobIds)
                .knowledgeJobId(knowledgeJobId)
                .build();
    }
}
