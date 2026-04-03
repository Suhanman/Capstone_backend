package com.emailagent.dto.response.admin.automation;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AdminAutomationRuleDetailResponse extends BaseResponse {

    @JsonProperty("rule_id")
    private final long ruleId;

    @JsonProperty("user_id")
    private final long userId;

    @JsonProperty("category_id")
    private final long categoryId;

    @JsonProperty("template_id")
    private final Long templateId;

    @JsonProperty("auto_send_enabled")
    private final boolean autoSendEnabled;

    @JsonProperty("auto_calendar_enabled")
    private final boolean autoCalendarEnabled;

    @JsonProperty("is_active")
    private final boolean active;

    public AdminAutomationRuleDetailResponse(long ruleId, long userId, long categoryId,
                                              Long templateId,
                                              boolean autoSendEnabled, boolean autoCalendarEnabled,
                                              boolean active) {
        this.ruleId = ruleId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.templateId = templateId;
        this.autoSendEnabled = autoSendEnabled;
        this.autoCalendarEnabled = autoCalendarEnabled;
        this.active = active;
    }
}
