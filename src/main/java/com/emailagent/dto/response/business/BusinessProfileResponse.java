package com.emailagent.dto.response.business;

import com.emailagent.domain.entity.BusinessProfile;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BusinessProfileResponse extends BaseResponse {

    @JsonProperty("industry_type")
    private String industryType;

    @JsonProperty("company_description")
    private String companyDescription;

    @JsonProperty("email_tone")
    private String emailTone;

    public static BusinessProfileResponse from(BusinessProfile profile) {
        return BusinessProfileResponse.builder()
                .industryType(profile.getIndustryType())
                .companyDescription(profile.getCompanyDescription())
                .emailTone(profile.getEmailTone() != null ? profile.getEmailTone().name() : null)
                .build();
    }
}
