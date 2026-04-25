package com.emailagent.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 온보딩 초기 템플릿 생성을 RAG worker에 요청하는 메시지 DTO.
 *
 * 기존 ai.draft는 이메일 답장 생성과 템플릿 생성을 함께 떠안던 초기안이었고,
 * 현재 기준으로 템플릿 생성은 RAG 책임으로 분리한다.
 */
@Getter
@Builder
public class RagDraftGenerateRequestDTO {

    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("mode")
    private String mode;

    @JsonProperty("category_id")
    private Long categoryId;

    @JsonProperty("category_name")
    private String categoryName;

    @JsonProperty("category_keywords")
    private List<String> categoryKeywords;

    @JsonProperty("industry_type")
    private String industryType;

    @JsonProperty("company_description")
    private String companyDescription;

    @JsonProperty("email_tone")
    private String emailTone;

    @JsonProperty("rag_context")
    private String ragContext;

    @JsonProperty("template_count")
    private Integer templateCount;
}
