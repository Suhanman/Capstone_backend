package com.emailagent.dto.request.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminAutomationRuleCreateRequest {

    @NotNull
    @JsonProperty("user_id")
    private Long userId;

    @NotNull
    @JsonProperty("category_id")
    private Long categoryId;

    @JsonProperty("template_id")
    private Long templateId;

    @NotNull
    @JsonProperty("auto_send_enabled")
    private Boolean autoSendEnabled;

    @NotNull
    @JsonProperty("auto_calendar_enabled")
    private Boolean autoCalendarEnabled;

    @JsonProperty("name")
    private String name;

    @JsonProperty("trigger_condition")
    private String triggerCondition;

    @JsonProperty("action_description")
    private String actionDescription;
}
