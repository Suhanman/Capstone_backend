package com.emailagent.service.admin;

import com.emailagent.domain.entity.BusinessProfile;
import com.emailagent.domain.entity.Template;
import com.emailagent.dto.response.admin.template.AdminTemplateCategoryStatResponse;
import com.emailagent.dto.response.admin.template.AdminTemplateListResponse;
import com.emailagent.dto.response.admin.template.AdminTemplateSummaryResponse;
import com.emailagent.repository.AutomationRuleRepository;
import com.emailagent.repository.BusinessProfileRepository;
import com.emailagent.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTemplateService {

    private final TemplateRepository templateRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final AutomationRuleRepository automationRuleRepository;

    /**
     * 전체 템플릿 목록 조회 (user_id 선택 필터, 페이징)
     */
    public AdminTemplateListResponse getTemplates(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size);

        Page<Template> templatePage = (userId != null)
                ? templateRepository.findByUserIdWithUserOrderByCreatedAtDesc(userId, pageable)
                : templateRepository.findAllWithUserOrderByCreatedAtDesc(pageable);

        // 배치 조회로 N+1 방지
        List<Long> userIds = templatePage.getContent().stream()
                .map(t -> t.getUser().getUserId())
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> industryMap = businessProfileRepository.findAllByUserIds(userIds).stream()
                .collect(Collectors.toMap(
                        bp -> bp.getUser().getUserId(),
                        bp -> bp.getIndustryType() != null ? bp.getIndustryType() : "",
                        (a, b) -> a
                ));

        List<AdminTemplateListResponse.TemplateItem> items = templatePage.getContent().stream()
                .map(t -> new AdminTemplateListResponse.TemplateItem(
                        t.getTemplateId(),
                        t.getUser().getUserId(),
                        t.getTitle(),
                        t.getCreatedAt().toInstant(ZoneOffset.UTC).toString(),
                        t.getCategory() != null ? t.getCategory().getCategoryName() : null,
                        industryMap.get(t.getUser().getUserId()),
                        t.getUseCount(),
                        t.getUserCount(),
                        t.getUpdatedAt(),
                        t.getQuality()
                ))
                .collect(Collectors.toList());

        return new AdminTemplateListResponse(templatePage.getTotalElements(), items);
    }

    /**
     * 템플릿 관리 페이지 요약 통계
     */
    public AdminTemplateSummaryResponse getSummary() {
        long totalTemplates = templateRepository.count();

        List<Object[]> categoryStats = templateRepository.findCategoryStatistics();
        String topCategory = null;
        long topCategoryUsageCount = 0;
        for (Object[] row : categoryStats) {
            long usageCount = ((Number) row[3]).longValue();
            if (usageCount > topCategoryUsageCount) {
                topCategoryUsageCount = usageCount;
                topCategory = (String) row[1];
            }
        }

        long activeRuleCount = automationRuleRepository.countByIsActiveTrue();
        long autoSendRuleCount = automationRuleRepository.countByAutoSendEnabledTrue();

        return new AdminTemplateSummaryResponse(
                totalTemplates, topCategory, topCategoryUsageCount,
                activeRuleCount, autoSendRuleCount
        );
    }

    /**
     * 카테고리별 템플릿 수 및 누적 사용 횟수 통계 조회
     * - TemplateUsageLogs 테이블과 Native JOIN 집계
     */
    public AdminTemplateCategoryStatResponse getCategoryStatistics() {
        List<Object[]> rows = templateRepository.findCategoryStatistics();

        List<AdminTemplateCategoryStatResponse.CategoryStat> stats = rows.stream()
                .map(row -> new AdminTemplateCategoryStatResponse.CategoryStat(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).longValue()
                ))
                .collect(Collectors.toList());

        return new AdminTemplateCategoryStatResponse(stats);
    }
}
