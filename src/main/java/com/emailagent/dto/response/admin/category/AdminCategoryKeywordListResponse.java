package com.emailagent.dto.response.admin.category;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class AdminCategoryKeywordListResponse extends BaseResponse {

    @JsonProperty("total_count")
    private final long totalCount;

    private final List<AdminCategoryKeywordItemResponse> categories;

    public AdminCategoryKeywordListResponse(List<AdminCategoryKeywordItemResponse> categories) {
        this.totalCount = categories.size();
        this.categories = categories;
    }
}
