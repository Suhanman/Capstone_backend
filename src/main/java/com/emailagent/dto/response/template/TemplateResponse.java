package com.emailagent.dto.response.template;

import com.emailagent.domain.entity.Template;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TemplateResponse extends BaseResponse {

    @JsonProperty("template_id")
    private Long templateId;

    @JsonProperty("category_id")
    private Long categoryId;

    @JsonProperty("category_name")
    private String categoryName;

    private String title;

    @JsonProperty("variant_label")
    private String variantLabel;

    @JsonProperty("subject_template")
    private String subjectTemplate;

    @JsonProperty("body_template")
    private String bodyTemplate;

    @JsonProperty("accuracy_score")
    private BigDecimal accuracyScore;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static TemplateResponse from(Template template) {
        return TemplateResponse.builder()
                .templateId(template.getTemplateId())
                .categoryId(template.getCategory().getCategoryId())
                .categoryName(template.getCategory().getCategoryName())
                .title(template.getTitle())
                .variantLabel(template.getVariantLabel())
                .subjectTemplate(template.getSubjectTemplate())
                .bodyTemplate(template.getBodyTemplate())
                .accuracyScore(template.getAccuracyScore())
                .createdAt(template.getCreatedAt())
                .build();
    }
}
