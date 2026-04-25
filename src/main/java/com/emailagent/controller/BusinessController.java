package com.emailagent.controller;

import com.emailagent.dto.request.business.*;
import com.emailagent.dto.response.auth.BaseResponse;
import com.emailagent.dto.response.business.*;
import com.emailagent.security.CurrentUser;
import com.emailagent.service.BusinessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    // =============================================
    // 비즈니스 프로필
    // GET  /api/business/profile
    // PUT  /api/business/profile
    // =============================================

    @GetMapping("/profile")
    public ResponseEntity<BusinessProfileResponse> getProfile(@CurrentUser Long userId) {
        BusinessProfileResponse response = businessService.getProfile(userId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<ProfileSaveResponse> upsertProfile(
            @CurrentUser Long userId,
            @Valid @RequestBody BusinessProfileRequest request) {
        return ResponseEntity.ok(businessService.upsertProfile(userId, request));
    }

    // =============================================
    // 비즈니스 파일
    // GET    /api/business/resources/files
    // POST   /api/business/resources/files
    // DELETE /api/business/resources/files/{resource_id}
    // =============================================

    @GetMapping("/resources/files")
    public ResponseEntity<BusinessResourceListResponse> getFiles(@CurrentUser Long userId) {
        return ResponseEntity.ok(businessService.getFiles(userId));
    }

    // 1단계: Presigned PUT URL 발급
    @PostMapping("/resources/presigned-url")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(
            @CurrentUser Long userId,
            @Valid @RequestBody PresignedUrlRequest request) {
        return ResponseEntity.ok(businessService.generatePresignedUrl(userId, request));
    }

    // 2단계: S3 직접 업로드 완료 후 메타데이터 저장 확정
    @PostMapping("/resources/files")
    public ResponseEntity<BusinessResourceResponse> confirmUpload(
            @CurrentUser Long userId,
            @Valid @RequestBody FileConfirmRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(businessService.confirmUpload(userId, request));
    }

    @DeleteMapping("/resources/files/{resourceId}")
    public ResponseEntity<BaseResponse> deleteFile(
            @CurrentUser Long userId,
            @PathVariable Long resourceId) {
        businessService.deleteFile(userId, resourceId);
        return ResponseEntity.ok(new BaseResponse());
    }

    // =============================================
    // FAQ
    // GET    /api/business/resources/faqs
    // POST   /api/business/resources/faqs
    // PUT    /api/business/resources/faqs/{faq_id}
    // DELETE /api/business/resources/faqs/{faq_id}
    // =============================================

    @GetMapping("/resources/faqs")
    public ResponseEntity<FaqListResponse> getFaqs(@CurrentUser Long userId) {
        return ResponseEntity.ok(businessService.getFaqs(userId));
    }

    @PostMapping("/resources/faqs")
    public ResponseEntity<FaqResponse> createFaq(
            @CurrentUser Long userId,
            @Valid @RequestBody FaqRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(businessService.createFaq(userId, request));
    }

    @PutMapping("/resources/faqs/{faqId}")
    public ResponseEntity<FaqResponse> updateFaq(
            @CurrentUser Long userId,
            @PathVariable Long faqId,
            @Valid @RequestBody FaqRequest request) {
        return ResponseEntity.ok(businessService.updateFaq(userId, faqId, request));
    }

    @DeleteMapping("/resources/faqs/{faqId}")
    public ResponseEntity<BaseResponse> deleteFaq(
            @CurrentUser Long userId,
            @PathVariable Long faqId) {
        businessService.deleteFaq(userId, faqId);
        return ResponseEntity.ok(new BaseResponse());
    }

    // =============================================
    // 카테고리
    // GET    /api/business/categories
    // POST   /api/business/categories
    // DELETE /api/business/categories/{category_id}
    // =============================================

    @GetMapping("/categories")
    public ResponseEntity<CategoryListResponse> getCategories(@CurrentUser Long userId) {
        return ResponseEntity.ok(businessService.getCategories(userId));
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(
            @CurrentUser Long userId,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(businessService.createCategory(userId, request));
    }

    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<BaseResponse> deleteCategory(
            @CurrentUser Long userId,
            @PathVariable Long categoryId) {
        businessService.deleteCategory(userId, categoryId);
        return ResponseEntity.ok(new BaseResponse());
    }

    // =============================================
    // 템플릿 재생성
    // POST /api/business/templates/regenerate
    // =============================================

    @PostMapping("/templates/regenerate")
    public ResponseEntity<TemplateRegenerateResponse> regenerateTemplates(
            @CurrentUser Long userId,
            @RequestBody TemplateRegenerateRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(businessService.regenerateTemplates(userId, request));
    }
}
