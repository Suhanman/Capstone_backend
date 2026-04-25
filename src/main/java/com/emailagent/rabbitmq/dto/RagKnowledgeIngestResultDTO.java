package com.emailagent.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG knowledge ingest 결과 DTO.
 */
@Getter
@NoArgsConstructor
public class RagKnowledgeIngestResultDTO {

    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("payload")
    private Payload payload;

    @JsonProperty("error")
    private ErrorPayload error;

    @Getter
    @NoArgsConstructor
    public static class Payload {
        @JsonProperty("indexed_document_count")
        private Integer indexedDocumentCount;

        @JsonProperty("indexed_chunk_count")
        private Integer indexedChunkCount;

        @JsonProperty("indexed_source_ids")
        private List<String> indexedSourceIds;
    }

    @Getter
    @NoArgsConstructor
    public static class ErrorPayload {
        @JsonProperty("code")
        private String code;

        @JsonProperty("message")
        private String message;
    }
}
