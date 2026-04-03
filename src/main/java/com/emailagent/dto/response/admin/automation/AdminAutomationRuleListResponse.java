package com.emailagent.dto.response.admin.automation;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

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

        public RuleItem(long ruleId, long userId, boolean active) {
            this.ruleId = ruleId;
            this.userId = userId;
            this.active = active;
        }
    }
}
