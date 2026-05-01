package com.emailagent.dto.request.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.List;

@Getter
public class AdminCategoryKeywordCreateRequest {

    @NotBlank
    @JsonProperty("category_name")
    private String categoryName;

    private String color;

    private List<String> keywords;
}
