package com.emailagent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class TemplateCreateRequest {
    @NotNull
    private Long categoryId;

    @NotBlank
    private String title;

    @NotBlank
    private String subjectTemplate;

    @NotBlank
    private String bodyTemplate;
}
