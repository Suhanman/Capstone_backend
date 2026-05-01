package com.emailagent.dto.response.admin.category;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class AdminCategoryKeywordItemResponse {

    @JsonProperty("category_key")
    private final String categoryKey;

    @JsonProperty("category_name")
    private final String categoryName;

    private final String color;

    private final List<String> keywords;

    @JsonProperty("category_count")
    private final int categoryCount;

    @JsonProperty("user_count")
    private final int userCount;

    public AdminCategoryKeywordItemResponse(
            String categoryName,
            String color,
            List<String> keywords,
            int categoryCount,
            int userCount
    ) {
        this.categoryKey = categoryName;
        this.categoryName = categoryName;
        this.color = color;
        this.keywords = keywords;
        this.categoryCount = categoryCount;
        this.userCount = userCount;
    }
}
