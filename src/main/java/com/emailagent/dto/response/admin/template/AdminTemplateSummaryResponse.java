package com.emailagent.dto.response.admin.template;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AdminTemplateSummaryResponse extends BaseResponse {

    @JsonProperty("total_templates")
    private final long totalTemplates;

    @JsonProperty("top_category")
    private final String topCategory;

    @JsonProperty("top_category_usage_count")
    private final long topCategoryUsageCount;

    @JsonProperty("active_rule_count")
    private final long activeRuleCount;

    @JsonProperty("auto_send_rule_count")
    private final long autoSendRuleCount;

    public AdminTemplateSummaryResponse(long totalTemplates, String topCategory,
                                         long topCategoryUsageCount, long activeRuleCount,
                                         long autoSendRuleCount) {
        this.totalTemplates = totalTemplates;
        this.topCategory = topCategory;
        this.topCategoryUsageCount = topCategoryUsageCount;
        this.activeRuleCount = activeRuleCount;
        this.autoSendRuleCount = autoSendRuleCount;
    }
}
