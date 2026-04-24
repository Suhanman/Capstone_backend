package com.emailagent.service.admin;

import com.emailagent.domain.entity.SupportTicket;
import com.emailagent.dto.response.admin.support.AdminSupportTicketDetailResponse;
import com.emailagent.dto.response.admin.support.AdminSupportTicketListResponse;
import com.emailagent.dto.response.admin.support.AdminSupportTicketReplyResponse;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.rabbitmq.event.SseEvent;
import com.emailagent.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminSupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 전체 문의 목록 조회 (status 필터, user_id 필터, 페이징)
     * - status와 user_id는 모두 선택 파라미터
     */
    @Transactional(readOnly = true)
    public AdminSupportTicketListResponse getTickets(String status, Long userId, int page, int size) {
        // page는 1-based로 입력받아 0-based로 변환
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size);

        Page<SupportTicket> ticketPage;
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasUserId = userId != null;

        if (hasUserId && hasStatus) {
            ticketPage = supportTicketRepository.findByUserIdAndStatusWithUserOrderByCreatedAtDesc(userId, status, pageable);
        } else if (hasUserId) {
            ticketPage = supportTicketRepository.findByUserIdWithUserOrderByCreatedAtDesc(userId, pageable);
        } else if (hasStatus) {
            ticketPage = supportTicketRepository.findByStatusWithUserOrderByCreatedAtDesc(status, pageable);
        } else {
            ticketPage = supportTicketRepository.findAllWithUserOrderByCreatedAtDesc(pageable);
        }

        List<AdminSupportTicketListResponse.TicketItem> items = ticketPage.getContent().stream()
                .map(t -> new AdminSupportTicketListResponse.TicketItem(
                        t.getTicketId(),
                        t.getUser().getUserId(),
                        t.getTitle(),
                        t.getStatus(),
                        t.getCreatedAt().toInstant(ZoneOffset.UTC).toString()
                ))
                .collect(Collectors.toList());

        return new AdminSupportTicketListResponse(ticketPage.getTotalElements(), items);
    }

    /**
     * 특정 문의 상세 조회
     */
    @Transactional(readOnly = true)
    public AdminSupportTicketDetailResponse getTicketDetail(Long ticketId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("문의를 찾을 수 없습니다. ticketId=" + ticketId));

        String repliedAt = ticket.getRepliedAt() != null
                ? ticket.getRepliedAt().toInstant(ZoneOffset.UTC).toString()
                : null;

        return new AdminSupportTicketDetailResponse(
                ticket.getTicketId(),
                ticket.getUser().getUserId(),
                ticket.getTitle(),
                ticket.getContent(),
                ticket.getStatus(),
                ticket.getAdminReply(),
                ticket.getRepliedBy(),
                repliedAt,
                ticket.getCreatedAt().toInstant(ZoneOffset.UTC).toString()
        );
    }

    /**
     * 관리자 답변 작성
     * - 상태를 ANSWERED로 변경, replied_by에 현재 관리자 userId 기록
     * - 이미 ANSWERED 상태인 문의도 답변 수정 허용 (덮어쓰기)
     */
    @Transactional
    public AdminSupportTicketReplyResponse replyToTicket(Long ticketId, String adminReply, Long adminUserId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("문의를 찾을 수 없습니다. ticketId=" + ticketId));

        ticket.registerAdminReply(adminReply, adminUserId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ticket_id", ticket.getTicketId());
        payload.put("status", ticket.getStatus());
        payload.put("admin_reply_present", ticket.getAdminReply() != null);
        payload.put(
                "updated_at",
                ticket.getRepliedAt() != null ? ticket.getRepliedAt().toInstant(ZoneOffset.UTC).toString() : null
        );

        eventPublisher.publishEvent(new SseEvent(
                this,
                ticket.getUser().getUserId(),
                "support-ticket-updated",
                payload
        ));

        return new AdminSupportTicketReplyResponse(ticket.getTicketId(), ticket.getStatus());
    }
}
