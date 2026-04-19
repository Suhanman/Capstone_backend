package com.emailagent.controller;

import com.emailagent.dto.request.inbox.CalendarActionRequest;
import com.emailagent.dto.request.inbox.RegenerateRequest;
import com.emailagent.dto.request.inbox.ReplyActionRequest;
import com.emailagent.dto.response.inbox.AttachmentDownloadResult;
import com.emailagent.dto.response.inbox.InboxActionResponse;
import com.emailagent.dto.response.inbox.InboxDetailResponse;
import com.emailagent.dto.response.inbox.InboxListResponse;
import com.emailagent.dto.response.inbox.RegenerateResponse;
import com.emailagent.security.CurrentUser;
import com.emailagent.service.InboxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

    // POST /api/inbox/{email_id}/regenerate
    @PostMapping("/{emailId}/regenerate")
    public ResponseEntity<RegenerateResponse> regenerate(
            @CurrentUser Long userId,
            @PathVariable Long emailId,
            @RequestBody RegenerateRequest request) {
        return ResponseEntity.ok(inboxService.regenerate(userId, emailId, request));
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
    // 성공: 바이너리 파일 스트림 반환 (BaseResponse 예외 적용 API)
    // 실패: GlobalExceptionHandler가 BaseResponse JSON으로 처리
    @GetMapping("/{emailId}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(
            @CurrentUser Long userId,
            @PathVariable Long emailId,
            @PathVariable int attachmentId) {
        AttachmentDownloadResult result = inboxService.downloadAttachment(userId, emailId, attachmentId);

        // 한글 파일명 깨짐 방지: RFC 5987 방식 (filename*=UTF-8''인코딩값)
        String encodedFileName = URLEncoder.encode(result.fileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String contentDisposition = "attachment; filename=\"" + result.fileName() + "\"; "
                + "filename*=UTF-8''" + encodedFileName;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(result.data());
    }

    // POST /api/inbox/test-seed
    @PostMapping("/test-seed")
    public ResponseEntity<?> seedTestEmail(@CurrentUser Long userId) {
        return ResponseEntity.ok(inboxService.seedTestEmail(userId));
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
