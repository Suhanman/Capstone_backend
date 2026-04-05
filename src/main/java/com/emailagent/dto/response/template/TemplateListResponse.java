package com.emailagent.dto.response.template;

import com.emailagent.dto.response.auth.BaseResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TemplateListResponse extends BaseResponse {

    private List<TemplateResponse> templates;
}
