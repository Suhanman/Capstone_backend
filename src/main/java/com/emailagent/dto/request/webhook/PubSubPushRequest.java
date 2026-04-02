package com.emailagent.dto.request.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Google Pub/Sub이 Push 방식으로 전송하는 표준 페이로드.
 * message.data 필드는 실제 알림 내용이 Base64 인코딩된 문자열로 들어온다.
 */
@Getter
@NoArgsConstructor
public class PubSubPushRequest {

    private Message message;
    private String subscription;

    @Getter
    @NoArgsConstructor
    public static class Message {

        /** Base64 인코딩된 알림 데이터 (디코딩 시 PubSubMessageData 구조) */
        private String data;

        @JsonProperty("messageId")
        private String messageId;

        @JsonProperty("publishTime")
        private String publishTime;
    }
}
