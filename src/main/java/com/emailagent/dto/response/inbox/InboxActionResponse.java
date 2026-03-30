package com.emailagent.dto.response.inbox;

import com.emailagent.dto.response.auth.BaseResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InboxActionResponse extends BaseResponse {

    private String message;
}
