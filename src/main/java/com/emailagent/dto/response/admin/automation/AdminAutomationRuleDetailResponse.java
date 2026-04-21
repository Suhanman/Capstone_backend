package com.emailagent.dto.response.admin.automation;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDateTime;

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

    private final String name;

    private final String category;

    private final String trigger;

    private final String action;

    private final String status;

    @JsonProperty("updated_at")
    private final LocalDateTime updatedAt;

    public AdminAutomationRuleDetailResponse(long ruleId, long userId, long categoryId,
                                              Long templateId,
                                              boolean autoSendEnabled, boolean autoCalendarEnabled,
                                              boolean active, String name, String category,
                                              String trigger, String action, LocalDateTime updatedAt) {
        this.ruleId = ruleId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.templateId = templateId;
        this.autoSendEnabled = autoSendEnabled;
        this.autoCalendarEnabled = autoCalendarEnabled;
        this.active = active;
        this.name = name;
        this.category = category;
        this.trigger = trigger;
        this.action = action;
        this.status = active ? "활성" : "비활성";
        this.updatedAt = updatedAt;
    }
}
