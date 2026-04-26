package com.emailagent.dto.request.admin;

import lombok.Getter;

import java.util.List;

@Getter
public class AdminCategoryKeywordUpdateRequest {

    private String color;

    private List<String> keywords;
}
