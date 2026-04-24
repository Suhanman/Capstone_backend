package com.emailagent.dto.response.admin.automation;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class AdminAutomationRuleListResponse extends BaseResponse {

    @JsonProperty("total_count")
    private final long totalCount;

    private final List<RuleItem> rules;

    public AdminAutomationRuleListResponse(long totalCount, List<RuleItem> rules) {
        this.totalCount = totalCount;
        this.rules = rules;
    }

    @Getter
    public static class RuleItem {

        @JsonProperty("rule_id")
        private final long ruleId;

        @JsonProperty("user_id")
        private final long userId;

        @JsonProperty("is_active")
        private final boolean active;

        private final String name;

        private final String category;

        private final String trigger;

        private final String action;

        private final String status;

        @JsonProperty("updated_at")
        private final LocalDateTime updatedAt;

        public RuleItem(long ruleId, long userId, boolean active,
                        String name, String category, String trigger, String action,
                        LocalDateTime updatedAt) {
            this.ruleId = ruleId;
            this.userId = userId;
            this.active = active;
            this.name = name;
            this.category = category;
            this.trigger = trigger;
            this.action = action;
            this.status = active ? "활성" : "비활성";
            this.updatedAt = updatedAt;
        }
    }
}
