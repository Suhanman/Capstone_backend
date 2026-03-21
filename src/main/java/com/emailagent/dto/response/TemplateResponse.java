package com.emailagent.dto.response;

import com.emailagent.domain.entity.Template;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TemplateResponse {
    private Long templateId;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String subjectTemplate;
    private String bodyTemplate;
    private BigDecimal accuracyScore;
    private LocalDateTime createdAt;

    public static TemplateResponse from(Template template) {
        return TemplateResponse.builder()
                .templateId(template.getTemplateId())
                .categoryId(template.getCategory().getCategoryId())
                .categoryName(template.getCategory().getCategoryName())
                .title(template.getTitle())
                .subjectTemplate(template.getSubjectTemplate())
                .bodyTemplate(template.getBodyTemplate())
                .accuracyScore(template.getAccuracyScore())
                .createdAt(template.getCreatedAt())
                .build();
    }
}
