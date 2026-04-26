package com.emailagent.controller.admin;

import com.emailagent.dto.request.admin.AdminCategoryKeywordCreateRequest;
import com.emailagent.dto.request.admin.AdminCategoryKeywordUpdateRequest;
import com.emailagent.dto.response.admin.AdminSimpleResponse;
import com.emailagent.dto.response.admin.category.AdminCategoryKeywordItemResponse;
import com.emailagent.dto.response.admin.category.AdminCategoryKeywordListResponse;
import com.emailagent.service.admin.AdminCategoryKeywordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryKeywordController {

    private final AdminCategoryKeywordService adminCategoryKeywordService;

    @GetMapping
    public AdminCategoryKeywordListResponse getCategories() {
        return adminCategoryKeywordService.getCategories();
    }

    @PostMapping
    public AdminCategoryKeywordItemResponse saveCategoryKeywords(
            @Valid @RequestBody AdminCategoryKeywordCreateRequest request
    ) {
        return adminCategoryKeywordService.saveCategoryKeywords(request);
    }

    @PatchMapping("/{category_name}")
    public AdminCategoryKeywordItemResponse updateCategoryKeywords(
            @PathVariable("category_name") String categoryName,
            @Valid @RequestBody AdminCategoryKeywordUpdateRequest request
    ) {
        return adminCategoryKeywordService.updateCategoryKeywords(categoryName, request);
    }

    @DeleteMapping("/{category_name}")
    public AdminSimpleResponse clearCategoryKeywords(@PathVariable("category_name") String categoryName) {
        return adminCategoryKeywordService.clearCategoryKeywords(categoryName);
    }
}
