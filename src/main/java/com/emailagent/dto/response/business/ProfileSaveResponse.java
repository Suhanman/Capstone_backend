package com.emailagent.dto.response.business;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileSaveResponse extends BaseResponse {

    @JsonProperty("profile_id")
    private Long profileId;
}
