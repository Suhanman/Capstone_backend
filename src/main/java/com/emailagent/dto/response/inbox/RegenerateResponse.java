package com.emailagent.dto.response.inbox;

import com.emailagent.dto.response.auth.BaseResponse;
import lombok.Getter;

@Getter
public class RegenerateResponse extends BaseResponse {

    private final Data data;

    public RegenerateResponse(String message) {
        super();
        this.data = new Data(message);
    }

    @Getter
    public static class Data {
        private final String message;

        public Data(String message) {
            this.message = message;
        }
    }
}