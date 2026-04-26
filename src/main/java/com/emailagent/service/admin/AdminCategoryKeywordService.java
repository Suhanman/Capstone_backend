package com.emailagent.service.admin;

import com.emailagent.domain.entity.Category;
import com.emailagent.dto.request.admin.AdminCategoryKeywordCreateRequest;
import com.emailagent.dto.request.admin.AdminCategoryKeywordUpdateRequest;
import com.emailagent.dto.response.admin.AdminSimpleResponse;
import com.emailagent.dto.response.admin.category.AdminCategoryKeywordItemResponse;
import com.emailagent.dto.response.admin.category.AdminCategoryKeywordListResponse;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.repository.CategoryRepository;
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

    @Transactional(readOnly = true)
    public AdminCategoryKeywordListResponse getCategories() {
        return new AdminCategoryKeywordListResponse(toGroupedResponses(
                categoryRepository.findAllWithUserOrderByCategoryNameAndUserId()
        ));
    }

    @Transactional
    public AdminCategoryKeywordItemResponse saveCategoryKeywords(AdminCategoryKeywordCreateRequest request) {
        String categoryName = normalizeRequired(request.getCategoryName(), "카테고리명은 필수입니다.");
        List<Category> categories = findCategoriesByName(categoryName);
        updateKeywordRows(categories, request.getColor(), request.getKeywords());
        return toGroupedResponse(categoryName, categories);
    }

    @Transactional
    public AdminCategoryKeywordItemResponse updateCategoryKeywords(
            String categoryName,
            AdminCategoryKeywordUpdateRequest request
    ) {
        String normalizedCategoryName = normalizeRequired(categoryName, "카테고리명은 필수입니다.");
        List<Category> categories = findCategoriesByName(normalizedCategoryName);
        updateKeywordRows(categories, request.getColor(), request.getKeywords());
        return toGroupedResponse(normalizedCategoryName, categories);
    }

    @Transactional
    public AdminSimpleResponse clearCategoryKeywords(String categoryName) {
        List<Category> categories = findCategoriesByName(normalizeRequired(categoryName, "카테고리명은 필수입니다."));
        categories.forEach(category -> category.updateKeywordsByAdmin(category.getColor(), List.of()));
        return AdminSimpleResponse.OK;
    }

    private List<Category> findCategoriesByName(String categoryName) {
        List<Category> categories = categoryRepository.findAllByCategoryNameWithUser(categoryName);
        if (categories.isEmpty()) {
            throw new ResourceNotFoundException("카테고리를 찾을 수 없습니다. categoryName=" + categoryName);
        }
        return categories;
    }

    private void updateKeywordRows(List<Category> categories, String color, List<String> keywords) {
        String normalizedColor = normalizeOptional(color);
        List<String> normalizedKeywords = normalizeKeywords(keywords);
        categories.forEach(category -> category.updateKeywordsByAdmin(normalizedColor, normalizedKeywords));
    }

    private List<AdminCategoryKeywordItemResponse> toGroupedResponses(List<Category> categories) {
        Map<String, List<Category>> grouped = new LinkedHashMap<>();
        categories.forEach(category ->
                grouped.computeIfAbsent(category.getCategoryName(), ignored -> new ArrayList<>()).add(category)
        );

        return grouped.entrySet().stream()
                .map(entry -> toGroupedResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private AdminCategoryKeywordItemResponse toGroupedResponse(String categoryName, List<Category> categories) {
        return new AdminCategoryKeywordItemResponse(
                categoryName,
                resolveColor(categories),
                mergeKeywords(categories),
                categories.size(),
                (int) categories.stream()
                        .map(category -> category.getUser().getUserId())
                        .filter(Objects::nonNull)
                        .distinct()
                        .count()
        );
    }

    private String resolveColor(List<Category> categories) {
        return categories.stream()
                .map(Category::getColor)
                .map(this::normalizeOptional)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private List<String> mergeKeywords(List<Category> categories) {
        LinkedHashSet<String> mergedKeywords = new LinkedHashSet<>();
        categories.forEach(category -> category.getKeywords().forEach(keyword -> {
            String normalized = normalizeOptional(keyword);
            if (normalized != null) {
                mergedKeywords.add(normalized);
            }
        }));
        return new ArrayList<>(mergedKeywords);
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
