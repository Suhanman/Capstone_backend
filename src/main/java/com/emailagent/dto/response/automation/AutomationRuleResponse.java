package com.emailagent.dto.response.automation;

import com.emailagent.domain.entity.AutomationRule;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AutomationRuleResponse extends BaseResponse {

    @JsonProperty("rule_id")
    private Long ruleId;

    private CategoryDto category;

    private List<String> keywords;

    private TemplateDto template;

    @JsonProperty("auto_send_enabled")
    private boolean autoSendEnabled;

    @JsonProperty("auto_calendar_enabled")
    private boolean autoCalendarEnabled;

    @Getter
    @Builder
    public static class CategoryDto {
        @JsonProperty("category_id")
        private Long categoryId;
        private String name;
        private String color;
    }

    @Getter
    @Builder
    public static class TemplateDto {
        @JsonProperty("template_id")
        private Long templateId;
        private String title;
    }

    /** 목록 응답 래퍼 */
    @Getter
    @Builder
    public static class ListResponse extends BaseResponse {
        private List<AutomationRuleResponse> data;
    }

    public static AutomationRuleResponse from(AutomationRule rule) {
        TemplateDto templateDto = rule.getTemplate() != null
                ? TemplateDto.builder()
                        .templateId(rule.getTemplate().getTemplateId())
                        .title(rule.getTemplate().getTitle())
                        .build()
                : null;

        return AutomationRuleResponse.builder()
                .ruleId(rule.getRuleId())
                .category(CategoryDto.builder()
                        .categoryId(rule.getCategory().getCategoryId())
                        .name(rule.getCategory().getCategoryName())
                        .color(rule.getCategory().getColor())
                        .build())
                .keywords(rule.getKeywords())
                .template(templateDto)
                .autoSendEnabled(rule.isAutoSendEnabled())
                .autoCalendarEnabled(rule.isAutoCalendarEnabled())
                .build();
    }
}
