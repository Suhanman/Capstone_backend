package com.emailagent.dto.request.business;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CategoryRequest {

    @NotBlank
    @JsonProperty("category_name")
    private String categoryName;

    private String color;
}
