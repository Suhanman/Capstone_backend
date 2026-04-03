package com.emailagent.dto.request.automation;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AutomationRuleRequest {

    @NotBlank(message = "카테고리명은 필수입니다.")
    @JsonProperty("category_name")
    private String categoryName;

    private String color;

    @JsonProperty("template_id")
    private Long templateId;

    @JsonProperty("auto_send_enabled")
    private boolean autoSendEnabled;
}
