package com.emailagent.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class CallbackResponse extends BaseResponse {

    @JsonProperty("is_gmail_connected")
    private final boolean isGmailConnected;

    @JsonProperty("is_calendar_connected")
    private final boolean isCalendarConnected;

    public CallbackResponse(boolean isGmailConnected, boolean isCalendarConnected) {
        this.isGmailConnected = isGmailConnected;
        this.isCalendarConnected = isCalendarConnected;
    }
}
