package com.emailagent.dto.response.onboarding;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OnboardingStatusResponse extends BaseResponse {

    @JsonProperty("onboarding_completed")
    private boolean onboardingCompleted;

    public static OnboardingStatusResponse of(boolean onboardingCompleted) {
        return OnboardingStatusResponse.builder()
                .onboardingCompleted(onboardingCompleted)
                .build();
    }
}
