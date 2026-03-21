package com.emailagent.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class TemplateUpdateRequest {
    @NotBlank private String title;
    @NotBlank private String subjectTemplate;
    @NotBlank private String bodyTemplate;
}
