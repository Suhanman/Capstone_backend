package com.emailagent.service.admin;

import com.emailagent.domain.entity.Category;
import com.emailagent.domain.entity.CategoryKeywordRule;
import com.emailagent.dto.request.admin.AdminCategoryKeywordCreateRequest;
import com.emailagent.dto.request.admin.AdminCategoryKeywordUpdateRequest;
import com.emailagent.dto.response.admin.AdminSimpleResponse;
import com.emailagent.dto.response.admin.category.AdminCategoryKeywordItemResponse;
import com.emailagent.dto.response.admin.category.AdminCategoryKeywordListResponse;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.repository.CategoryKeywordRuleRepository;
import com.emailagent.repository.CategoryRepository;
import com.emailagent.service.RagTemplateIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminCategoryKeywordService {

    private final CategoryRepository categoryRepository;
    private final CategoryKeywordRuleRepository keywordRuleRepository;
    private final RagTemplateIndexService ragTemplateIndexService;

    @Transactional(readOnly = true)
    public AdminCategoryKeywordListResponse getCategories() {
        return new AdminCategoryKeywordListResponse(toGroupedResponses());
    }

    @Transactional
    public AdminCategoryKeywordItemResponse saveCategoryKeywords(AdminCategoryKeywordCreateRequest request) {
        String categoryName = normalizeRequired(request.getCategoryName(), "카테고리명은 필수입니다.");
        List<Category> categories = categoryRepository.findAllByCategoryNameWithUser(categoryName);
        ensureKnownCategory(categoryName, categories);
        CategoryKeywordRule rule = saveRule(categoryName, request.getColor(), request.getKeywords());
        ragTemplateIndexService.reindexCategories(categories);
        return toGroupedResponse(categoryName, rule, categories);
    }

    @Transactional
    public AdminCategoryKeywordItemResponse updateCategoryKeywords(
            String categoryName,
            AdminCategoryKeywordUpdateRequest request
    ) {
        String normalizedCategoryName = normalizeRequired(categoryName, "카테고리명은 필수입니다.");
        CategoryKeywordRule rule = saveRule(normalizedCategoryName, request.getColor(), request.getKeywords());
        List<Category> categories = categoryRepository.findAllByCategoryNameWithUser(normalizedCategoryName);
        ragTemplateIndexService.reindexCategories(categories);
        return toGroupedResponse(normalizedCategoryName, rule, categories);
    }

    @Transactional
    public AdminSimpleResponse clearCategoryKeywords(String categoryName) {
        String normalizedCategoryName = normalizeRequired(categoryName, "카테고리명은 필수입니다.");
        keywordRuleRepository.findByCategoryName(normalizedCategoryName)
                .ifPresent(keywordRuleRepository::delete);
        List<Category> categories = categoryRepository.findAllByCategoryNameWithUser(normalizedCategoryName);
        ragTemplateIndexService.reindexCategories(categories);
        return AdminSimpleResponse.OK;
    }

    private CategoryKeywordRule saveRule(String categoryName, String color, List<String> keywords) {
        CategoryKeywordRule rule = keywordRuleRepository.findByCategoryName(categoryName)
                .orElseGet(() -> CategoryKeywordRule.builder()
                        .categoryName(categoryName)
                        .build());
        rule.update(normalizeOptional(color), normalizeKeywords(keywords));
        return keywordRuleRepository.save(rule);
    }

    private void ensureKnownCategory(String categoryName, List<Category> categories) {
        if (!categories.isEmpty() || keywordRuleRepository.findByCategoryName(categoryName).isPresent()) {
            return;
        }
        throw new ResourceNotFoundException("등록된 운영 카테고리를 찾을 수 없습니다. categoryName=" + categoryName);
    }

    private List<AdminCategoryKeywordItemResponse> toGroupedResponses() {
        Map<String, List<Category>> categoriesByName = groupCategories(
                categoryRepository.findAllWithUserOrderByCategoryNameAndUserId()
        );
        Map<String, CategoryKeywordRule> rulesByName = groupRules(keywordRuleRepository.findAllByOrderByCategoryNameAsc());

        LinkedHashSet<String> categoryNames = new LinkedHashSet<>();
        categoryNames.addAll(rulesByName.keySet());

        return categoryNames.stream()
                .map(categoryName -> toGroupedResponse(
                        categoryName,
                        rulesByName.get(categoryName),
                        categoriesByName.getOrDefault(categoryName, List.of())
                ))
                .toList();
    }

    private Map<String, List<Category>> groupCategories(List<Category> categories) {
        Map<String, List<Category>> grouped = new LinkedHashMap<>();
        categories.forEach(category ->
                grouped.computeIfAbsent(category.getCategoryName(), ignored -> new ArrayList<>()).add(category)
        );
        return grouped;
    }

    private Map<String, CategoryKeywordRule> groupRules(List<CategoryKeywordRule> rules) {
        Map<String, CategoryKeywordRule> grouped = new LinkedHashMap<>();
        rules.forEach(rule -> grouped.put(rule.getCategoryName(), rule));
        return grouped;
    }

    private AdminCategoryKeywordItemResponse toGroupedResponse(
            String categoryName,
            CategoryKeywordRule rule,
            List<Category> categories
    ) {
        return new AdminCategoryKeywordItemResponse(
                categoryName,
                resolveColor(rule, categories),
                rule != null ? rule.getKeywords() : List.of(),
                categories.size(),
                (int) categories.stream()
                        .map(category -> category.getUser().getUserId())
                        .filter(Objects::nonNull)
                        .distinct()
                        .count()
        );
    }

    private String resolveColor(CategoryKeywordRule rule, List<Category> categories) {
        if (rule != null) {
            String ruleColor = normalizeOptional(rule.getColor());
            if (ruleColor != null) {
                return ruleColor;
            }
        }

        return categories.stream()
                .map(Category::getColor)
                .map(this::normalizeOptional)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private List<String> normalizeKeywords(List<String> keywords) {
        if (keywords == null) {
            return new ArrayList<>();
        }

        LinkedHashSet<String> uniqueKeywords = new LinkedHashSet<>();
        for (String keyword : keywords) {
            String normalized = normalizeOptional(keyword);
            if (normalized != null) {
                uniqueKeywords.add(normalized);
            }
        }

        return new ArrayList<>(uniqueKeywords);
    }
}
