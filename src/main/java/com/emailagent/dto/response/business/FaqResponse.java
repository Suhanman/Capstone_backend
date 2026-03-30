package com.emailagent.dto.response.business;

import com.emailagent.domain.entity.BusinessFaq;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FaqResponse extends BaseResponse {

    @JsonProperty("faq_id")
    private Long faqId;

    private String question;
    private String answer;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static FaqResponse from(BusinessFaq faq) {
        return FaqResponse.builder()
                .faqId(faq.getFaqId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .createdAt(faq.getCreatedAt())
                .build();
    }
}
