package com.emailagent.rabbitmq.controller;

import com.emailagent.dto.response.admin.operation.AdminJobDeleteResponse;
import com.emailagent.dto.response.admin.operation.AdminJobDetailResponse;
import com.emailagent.dto.response.admin.operation.AdminJobListResponse;
import com.emailagent.dto.response.admin.operation.AdminJobSummaryResponse;
import com.emailagent.rabbitmq.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 Job(Outbox) 모니터링 API.
 *
 * GET    /api/admin/operations/jobs           — 전체 Job 목록 (상태 필터, 페이징)
 * GET    /api/admin/operations/jobs/summary   — 상태별 건수 요약
 * GET    /api/admin/operations/jobs/{job_id}  — Job 상세
 * DELETE /api/admin/operations/jobs/{job_id}  — 관리자 강제 삭제
 */
@RestController
@RequestMapping("/api/admin/operations/mail-jobs")
@RequiredArgsConstructor
public class MailController {

    private final MailService mailService;

    @GetMapping
    public ResponseEntity<AdminJobListResponse> getJobList(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(mailService.getJobList(status, pageable));
    }

    @GetMapping("/summary")
    public ResponseEntity<AdminJobSummaryResponse> getJobSummary() {
        return ResponseEntity.ok(mailService.getJobSummary());
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<AdminJobDetailResponse> getJobDetail(@PathVariable Long jobId) {
        return ResponseEntity.ok(mailService.getJobDetail(jobId));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<AdminJobDeleteResponse> deleteJob(@PathVariable Long jobId) {
        mailService.forceDelete(jobId);
        return ResponseEntity.ok(new AdminJobDeleteResponse());
    }
}
