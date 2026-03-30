package com.emailagent.dto.response.support;

import com.emailagent.domain.entity.SupportTicket;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SupportTicketDetailResponse extends BaseResponse {

    @JsonProperty("ticket_id")
    private Long ticketId;

    private String title;
    private String content;
    private String status;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("admin_reply")
    private String adminReply; // 답변 없으면 null

    public static SupportTicketDetailResponse from(SupportTicket ticket) {
        return SupportTicketDetailResponse.builder()
                .ticketId(ticket.getTicketId())
                .title(ticket.getTitle())
                .content(ticket.getContent())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .adminReply(ticket.getAdminReply())
                .build();
    }

    // POST 응답: { "content_type": "application/json", "success": true, "result_code": 200, "result_req": "", "ticket_id": 1001 }
    @Getter
    @Builder
    public static class CreateResponse extends BaseResponse {

        @JsonProperty("ticket_id")
        private Long ticketId;
    }
}
