package com.emailagent.service;

import com.emailagent.domain.entity.Category;
import com.emailagent.domain.entity.Template;
import com.emailagent.rabbitmq.dto.RagTemplateIndexRequestDTO;
import com.emailagent.rabbitmq.publisher.RagPublisher;
import com.emailagent.repository.BusinessProfileRepository;
import com.emailagent.repository.TemplateRepository;
import com.emailagent.util.CategoryKeywordDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RagTemplateIndexService {

    private final TemplateRepository templateRepository;
    private final BusinessProfileRepository profileRepository;
    private final RagPublisher ragPublisher;

    public void reindexCategories(List<Category> categories) {
        Map<Long, Category> uniqueCategories = new LinkedHashMap<>();
        categories.forEach(category -> uniqueCategories.put(category.getCategoryId(), category));
        uniqueCategories.values().forEach(this::reindexCategory);
    }

    public void reindexCategory(Category category) {
        List<Template> templates = templateRepository.findByCategory_CategoryId(category.getCategoryId());
        if (templates.isEmpty()) {
            return;
        }

        String requestId = "template-index-" + category.getCategoryId() + "-" + System.currentTimeMillis();
        String emailTone = profileRepository.findByUser_UserId(category.getUser().getUserId())
                .map(profile -> profile.getEmailTone() != null ? profile.getEmailTone().name() : null)
                .orElse(null);

        List<RagTemplateIndexRequestDTO.TemplateItem> indexItems = templates.stream()
                .map(template -> toIndexItem(template, category, emailTone))
                .toList();

        ragPublisher.publishTemplateIndex(RagTemplateIndexRequestDTO.builder()
                .requestId(requestId)
                .userId(category.getUser().getUserId())
                .payload(RagTemplateIndexRequestDTO.Payload.builder()
                        .templates(indexItems)
                        .build())
                .build());
    }

    private RagTemplateIndexRequestDTO.TemplateItem toIndexItem(
            Template template,
            Category category,
            String emailTone
    ) {
        String variantLabel = template.getVariantLabel() != null ? template.getVariantLabel() : "일반형";
        List<String> semanticKeywords = new ArrayList<>();
        semanticKeywords.add(category.getCategoryName());
        semanticKeywords.add(variantLabel);
        semanticKeywords.addAll(CategoryKeywordDefaults.resolve(category.getCategoryName(), category.getKeywords()));

        return RagTemplateIndexRequestDTO.TemplateItem.builder()
                .templateId(template.getTemplateId())
                .title(template.getTitle())
                .categoryName(category.getCategoryName())
                .emailTone(emailTone)
                .metadata(RagTemplateIndexRequestDTO.Metadata.builder()
                        .searchSummary(variantLabel + " 템플릿")
                        .semanticKeywords(CategoryKeywordDefaults.normalize(semanticKeywords))
                        .recommendedSituations(List.of())
                        .build())
                .build();
    }
}
