package com.emailagent.controller;

import com.emailagent.dto.response.EmailDetailResponse;
import com.emailagent.dto.response.EmailPageResponse;
import com.emailagent.security.CurrentUser;
import com.emailagent.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    // GET /api/emails?page=0&size=20
    @GetMapping
    public ResponseEntity<EmailPageResponse> getEmails(
            @CurrentUser Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(emailService.getEmails(userId, pageable));
    }

    // GET /api/emails/{emailId}
    @GetMapping("/{emailId}")
    public ResponseEntity<EmailDetailResponse> getEmail(
            @PathVariable Long emailId,
            @CurrentUser Long userId) {
        return ResponseEntity.ok(emailService.getEmailDetail(emailId, userId));
    }
}
