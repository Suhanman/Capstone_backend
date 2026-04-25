package com.emailagent.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * FAQ / 업로드 PDF를 RAG knowledge index에 적재하기 위한 요청 메시지 DTO.
 *
 * RAG 쪽 ingest 계약에 맞춰 manual 입력은 로컬 테스트용 local_path 또는 운영용 presigned_url을 사용한다.
 */
@Getter
@Builder
public class RagKnowledgeIngestRequestDTO {

    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("payload")
    private Payload payload;

    @Getter
    @Builder
    public static class Payload {
        @JsonProperty("faqs")
        private List<FaqItem> faqs;

        @JsonProperty("manuals")
        private List<ManualItem> manuals;
    }

    @Getter
    @Builder
    public static class FaqItem {
        @JsonProperty("source_id")
        private String sourceId;

        @JsonProperty("question")
        private String question;

        @JsonProperty("answer")
        private String answer;
    }

    @Getter
    @Builder
    public static class ManualItem {
        @JsonProperty("source_id")
        private String sourceId;

        @JsonProperty("title")
        private String title;

        @JsonProperty("file_name")
        private String fileName;

        @JsonProperty("local_path")
        private String localPath;

        @JsonProperty("presigned_url")
        private String presignedUrl;
    }
}
