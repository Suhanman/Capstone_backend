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
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    // GET /api/inbox/{email_id}/attachments/{attachment_id}
    @GetMapping("/{emailId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(
            @CurrentUser Long userId,
            @PathVariable Long emailId,
            @PathVariable Long attachmentId) {
        Resource resource = inboxService.downloadAttachment(userId, emailId, attachmentId);

        // 파일명과 MIME 타입은 Resource의 메타정보를 활용
        // (실제 운영에서는 DB의 fileName/mimeType 기반으로 헤더 구성)
        String filename = resource.getFilename() != null ? resource.getFilename() : "attachment";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(resource);
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
