package com.emailagent.dto.request.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Pub/Sub message.data를 Base64 디코딩한 실제 Gmail 알림 데이터.
 * Google이 보내는 표준 형식:
 * { "emailAddress": "user@gmail.com", "historyId": 12345 }
 */
@Getter
@NoArgsConstructor
public class PubSubMessageData {

    @JsonProperty("emailAddress")
    private String emailAddress;

    @JsonProperty("historyId")
    private Long historyId;
}
