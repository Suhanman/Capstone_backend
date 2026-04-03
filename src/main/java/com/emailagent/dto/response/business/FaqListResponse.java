package com.emailagent.dto.response.business;

import com.emailagent.dto.response.auth.BaseResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FaqListResponse extends BaseResponse {

    private List<FaqResponse> faqs;
}
