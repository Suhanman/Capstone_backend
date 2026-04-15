package com.emailagent.controller;

import com.emailagent.dto.request.onboarding.InitialTemplateGenerateRequest;
import com.emailagent.dto.response.auth.BaseResponse;
import com.emailagent.dto.response.onboarding.InitialTemplateGenerateResponse;
import com.emailagent.dto.response.onboarding.OnboardingStatusResponse;
import com.emailagent.dto.response.onboarding.OnboardingTemplateJobStatusResponse;
import com.emailagent.security.CurrentUser;
import com.emailagent.service.RagJobService;
import com.emailagent.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final RagJobService ragJobService;

    // GET /api/onboarding/status
    @GetMapping("/api/onboarding/status")
    public ResponseEntity<OnboardingStatusResponse> getOnboardingStatus(@CurrentUser Long userId) {
        return ResponseEntity.ok(onboardingService.getOnboardingStatus(userId));
    }

    // POST /api/onboarding/complete
    @PostMapping("/api/onboarding/complete")
    public ResponseEntity<BaseResponse> completeOnboarding(@CurrentUser Long userId) {
        onboardingService.completeOnboarding(userId);
        return ResponseEntity.ok(new BaseResponse());
    }

    // POST /api/business/templates/generate-initial
    @PostMapping("/api/business/templates/generate-initial")
    public ResponseEntity<InitialTemplateGenerateResponse> generateInitialTemplates(
            @CurrentUser Long userId,
            @RequestBody InitialTemplateGenerateRequest request) {
        return ResponseEntity.ok(onboardingService.generateInitialTemplates(userId, request));
    }

    @GetMapping("/api/onboarding/template-jobs")
    public ResponseEntity<OnboardingTemplateJobStatusResponse> getTemplateGenerationJobs(
            @CurrentUser Long userId,
            @RequestParam(name = "job_ids") String jobIdsParam) {
        List<String> jobIds = Arrays.stream(jobIdsParam.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();

        return ResponseEntity.ok(ragJobService.getTemplateGenerationJobs(userId, jobIds));
    }
}
