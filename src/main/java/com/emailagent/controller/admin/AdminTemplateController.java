package com.emailagent.controller.admin;

import com.emailagent.dto.response.admin.template.AdminTemplateCategoryStatResponse;
import com.emailagent.dto.response.admin.template.AdminTemplateListResponse;
import com.emailagent.dto.response.admin.template.AdminTemplateSummaryResponse;
import com.emailagent.service.admin.AdminTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/templates")
@RequiredArgsConstructor
public class AdminTemplateController {

    private final AdminTemplateService adminTemplateService;

    @GetMapping
    public AdminTemplateListResponse getTemplates(
            @RequestParam(value = "user_id", required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminTemplateService.getTemplates(userId, page, size);
    }

    @GetMapping("/summary")
    public AdminTemplateSummaryResponse getSummary() {
        return adminTemplateService.getSummary();
    }

    @GetMapping("/statistics/by-category")
    public AdminTemplateCategoryStatResponse getCategoryStatistics() {
        return adminTemplateService.getCategoryStatistics();
    }
}
