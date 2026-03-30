package com.emailagent.controller;

import com.emailagent.dto.request.template.TemplateCreateRequest;
import com.emailagent.dto.request.template.TemplateUpdateRequest;
import com.emailagent.dto.response.auth.BaseResponse;
import com.emailagent.dto.response.template.TemplateListResponse;
import com.emailagent.dto.response.template.TemplateResponse;
import com.emailagent.security.CurrentUser;
import com.emailagent.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public ResponseEntity<TemplateListResponse> getTemplates(@CurrentUser Long userId) {
        return ResponseEntity.ok(templateService.getTemplates(userId));
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(
            @CurrentUser Long userId,
            @Valid @RequestBody TemplateCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(templateService.createTemplate(userId, request));
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<TemplateResponse> updateTemplate(
            @PathVariable Long templateId,
            @CurrentUser Long userId,
            @Valid @RequestBody TemplateUpdateRequest request) {
        return ResponseEntity.ok(templateService.updateTemplate(templateId, userId, request));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<BaseResponse> deleteTemplate(
            @PathVariable Long templateId,
            @CurrentUser Long userId) {
        templateService.deleteTemplate(templateId, userId);
        return ResponseEntity.ok(new BaseResponse());
    }
}
