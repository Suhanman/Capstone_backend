package com.emailagent.dto.response.inbox;

import com.emailagent.dto.response.auth.BaseResponse;
import lombok.Getter;

@Getter
public class RegenerateResponse extends BaseResponse {

    private final String message;

    public RegenerateResponse(String message) {
        super();
        this.message = message;
    }
}
