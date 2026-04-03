package com.emailagent.dto.response.support;

import com.emailagent.domain.entity.SupportTicket;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SupportTicketListResponse extends BaseResponse {

    private List<TicketSummary> tickets;

    @Getter
    @Builder
    public static class TicketSummary {

        @JsonProperty("ticket_id")
        private Long ticketId;

        private String title;

        // content 앞 100자만 노출
        @JsonProperty("content_preview")
        private String contentPreview;

        private String status;

        @JsonProperty("created_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        public static TicketSummary from(SupportTicket ticket) {
            String content = ticket.getContent();
            String preview = content.length() > 100 ? content.substring(0, 100) + "..." : content;

            return TicketSummary.builder()
                    .ticketId(ticket.getTicketId())
                    .title(ticket.getTitle())
                    .contentPreview(preview)
                    .status(ticket.getStatus())
                    .createdAt(ticket.getCreatedAt())
                    .build();
        }
    }

    public static SupportTicketListResponse of(List<SupportTicket> tickets) {
        return SupportTicketListResponse.builder()
                .tickets(tickets.stream().map(TicketSummary::from).toList())
                .build();
    }
}
