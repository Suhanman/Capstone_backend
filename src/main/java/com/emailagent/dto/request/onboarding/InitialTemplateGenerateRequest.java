package com.emailagent.dto.request.onboarding;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class InitialTemplateGenerateRequest {

    @JsonProperty("industry_type")
    private String industryType;

    @JsonProperty("email_tone")
    private String emailTone;

    @JsonProperty("company_description")
    private String companyDescription;

    @JsonProperty("category_ids")
    private List<Long> categoryIds;

    @JsonProperty("faq_ids")
    private List<Long> faqIds;

    @JsonProperty("resource_ids")
    private List<Long> resourceIds;
}
