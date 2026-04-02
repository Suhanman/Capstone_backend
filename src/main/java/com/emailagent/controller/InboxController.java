package com.emailagent.controller;

import com.emailagent.dto.request.inbox.CalendarActionRequest;
import com.emailagent.dto.request.inbox.RegenerateRequest;
import com.emailagent.dto.request.inbox.ReplyActionRequest;
import com.emailagent.dto.response.inbox.InboxActionResponse;
import com.emailagent.dto.response.inbox.InboxDetailResponse;
import com.emailagent.dto.response.inbox.InboxListResponse;
import com.emailagent.dto.response.inbox.RegenerateResponse;
import com.emailagent.dto.response.recommend.RecommendedDraftResponse;
import com.emailagent.security.CurrentUser;
import com.emailagent.service.InboxService;
import com.emailagent.service.RecommendService;
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
    private final RecommendService recommendService;

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

    // POST /api/inbox/{email_id}/regenerate
    @PostMapping("/{emailId}/regenerate")
    public ResponseEntity<RegenerateResponse> regenerate(
            @CurrentUser Long userId,
            @PathVariable Long emailId,
            @RequestBody RegenerateRequest request) {
        return ResponseEntity.ok(inboxService.regenerate(userId, emailId, request));
    }

    // GET /api/inbox/{email_id}/recommendations?topK=3
    @GetMapping("/{emailId}/recommendations")
    public ResponseEntity<RecommendedDraftResponse> getRecommendations(
            @CurrentUser Long userId,
            @PathVariable Long emailId,
            @RequestParam(defaultValue = "3") int topK) {
        return ResponseEntity.ok(recommendService.recommendSimilarDrafts(emailId, topK));
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
