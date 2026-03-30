package com.emailagent.dto.response.business;

import com.emailagent.domain.entity.BusinessResource;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BusinessResourceResponse extends BaseResponse {

    @JsonProperty("resource_id")
    private Long resourceId;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("file_type")
    private String fileType;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static BusinessResourceResponse from(BusinessResource resource) {
        return BusinessResourceResponse.builder()
                .resourceId(resource.getResourceId())
                .fileName(resource.getFileName())
                .fileType(resource.getFileType())
                .createdAt(resource.getCreatedAt())
                .build();
    }
}
