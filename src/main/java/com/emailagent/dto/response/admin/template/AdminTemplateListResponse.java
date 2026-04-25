package com.emailagent.dto.response.admin.template;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class AdminTemplateListResponse extends BaseResponse {

    @JsonProperty("total_count")
    private final long totalCount;

    private final List<TemplateItem> templates;

    public AdminTemplateListResponse(long totalCount, List<TemplateItem> templates) {
        this.totalCount = totalCount;
        this.templates = templates;
    }

    @Getter
    public static class TemplateItem {

        @JsonProperty("template_id")
        private final long templateId;

        @JsonProperty("user_template_no")
        private final Long userTemplateNo;

        @JsonProperty("user_id")
        private final long userId;

        private final String title;

        @JsonProperty("created_at")
        private final String createdAt;

        private final String category;

        private final String industry;

        @JsonProperty("use_count")
        private final Integer useCount;

        @JsonProperty("user_count")
        private final Integer userCount;

        @JsonProperty("generated_at")
        private final LocalDateTime generatedAt;

        private final String quality;

        public TemplateItem(long templateId, Long userTemplateNo, long userId, String title, String createdAt,
                            String category, String industry, Integer useCount, Integer userCount,
                            LocalDateTime generatedAt, String quality) {
            this.templateId = templateId;
            this.userTemplateNo = userTemplateNo;
            this.userId = userId;
            this.title = title;
            this.createdAt = createdAt;
            this.category = category;
            this.industry = industry;
            this.useCount = useCount;
            this.userCount = userCount;
            this.generatedAt = generatedAt;
            this.quality = quality;
        }
    }
}
