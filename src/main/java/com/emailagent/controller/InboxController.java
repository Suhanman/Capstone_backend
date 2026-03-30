package com.emailagent.controller;

import com.emailagent.dto.request.inbox.CalendarActionRequest;
import com.emailagent.dto.request.inbox.ReplyActionRequest;
import com.emailagent.dto.response.inbox.InboxActionResponse;
import com.emailagent.dto.response.inbox.InboxDetailResponse;
import com.emailagent.dto.response.inbox.InboxListResponse;
import com.emailagent.security.CurrentUser;
import com.emailagent.service.InboxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inbox")
@RequiredArgsConstructor
public class InboxController {

    private final InboxService inboxService;

    // GET /api/inbox?page=0&size=20&status=PENDING_REVIEW
    @GetMapping
    public ResponseEntity<InboxListResponse> getInbox(
            @CurrentUser Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String status) {
        return ResponseEntity.ok(inboxService.getInbox(userId, page, size, status));
    }

    // GET /api/inbox/{email_id}
    @GetMapping("/{emailId}")
    public ResponseEntity<InboxDetailResponse> getDetail(
            @CurrentUser Long userId,
            @PathVariable Long emailId) {
        return ResponseEntity.ok(inboxService.getDetail(userId, emailId));
    }

    // POST /api/inbox/{email_id}/reply
    @PostMapping("/{emailId}/reply")
    public ResponseEntity<InboxActionResponse> processReply(
            @CurrentUser Long userId,
            @PathVariable Long emailId,
            @Valid @RequestBody ReplyActionRequest request) {
        return ResponseEntity.ok(inboxService.processReply(userId, emailId, request));
    }

    // POST /api/inbox/{email_id}/calendar
    @PostMapping("/{emailId}/calendar")
    public ResponseEntity<InboxActionResponse> processCalendar(
            @CurrentUser Long userId,
            @PathVariable Long emailId,
            @Valid @RequestBody CalendarActionRequest request) {
        return ResponseEntity.ok(inboxService.processCalendar(userId, emailId, request));
    }
}
