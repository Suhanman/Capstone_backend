package com.emailagent.dto.request.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminAutomationRuleUpdateRequest {

    // 모든 필드 선택사항 — null이면 해당 필드 변경 안 함
    @JsonProperty("template_id")
    private Long templateId;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("auto_send_enabled")
    private Boolean autoSendEnabled;

    @JsonProperty("name")
    private String name;

    @JsonProperty("trigger_condition")
    private String triggerCondition;

    @JsonProperty("action_description")
    private String actionDescription;
}
