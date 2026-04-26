package com.emailagent.service;

import com.emailagent.domain.entity.Category;
import com.emailagent.domain.entity.Email;
import com.emailagent.domain.entity.EmailTemplateRecommendation;
import com.emailagent.domain.entity.Template;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.rabbitmq.dto.RagDraftGenerateResultDTO;
import com.emailagent.rabbitmq.dto.RagTemplateMatchResultDTO;
import com.emailagent.rabbitmq.dto.RagTemplateIndexRequestDTO;
import com.emailagent.rabbitmq.event.SseEvent;
import com.emailagent.rabbitmq.publisher.RagPublisher;
import com.emailagent.repository.BusinessProfileRepository;
import com.emailagent.repository.CategoryRepository;
import com.emailagent.repository.EmailRepository;
import com.emailagent.repository.EmailTemplateRecommendationRepository;
import com.emailagent.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 결과 메시지를 백엔드 도메인 모델에 반영하는 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagResultService {

    private final TemplateRepository templateRepository;
    private final CategoryRepository categoryRepository;
    private final BusinessProfileRepository profileRepository;
    private final EmailRepository emailRepository;
    private final EmailTemplateRecommendationRepository recommendationRepository;
    private final TemplateNumberService templateNumberService;
    private final RagPublisher ragPublisher;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void handleDraftGenerated(RagDraftGenerateResultDTO result) {
        if (!"SUCCESS".equalsIgnoreCase(result.getStatus())) {
            String message = result.getError() != null ? result.getError().getMessage() : "RAG draft 생성 실패";
            log.warn(
                    "[RagResultService] draft result 실패 응답 수신 — requestId={}, jobId={}, message={}",
                    result.getRequestId(),
                    result.getJobId(),
                    message
            );
            return;
        }

        RagDraftGenerateResultDTO.Payload payload = result.getPayload();
        if (payload == null) {
            throw new IllegalArgumentException("RAG draft 결과 payload가 없습니다.");
        }

        Long userId = result.getUserId();
        Long categoryId = payload.getCategoryId();

        Category category = categoryRepository.findById(categoryId)
                .filter(found -> found.getUser().getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("카테고리를 찾을 수 없습니다: " + categoryId));

        List<RagDraftGenerateResultDTO.TemplateItem> items = payload.getTemplates();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("RAG draft 결과 payload.templates가 없습니다.");
        }

        List<Template> savedTemplates = items.stream()
                .map(item -> upsertTemplate(userId, category, item))
                .toList();

        publishTemplateIndex(savedTemplates, category, items);

        // 같은 category_name을 공유하는 모든 템플릿의 user_count 갱신
        templateRepository.findUserCountPerTemplate().forEach(row -> {
            Long templateId = ((Number) row[0]).longValue();
            int count = ((Number) row[1]).intValue();
            templateRepository.findById(templateId).ifPresent(t -> t.updateUserCount(count));
        });

        log.info(
                "[RagResultService] draft 결과로 템플릿 저장 완료 — userId={}, categoryId={}, count={}",
                userId,
                categoryId,
                savedTemplates.size()
        );
    }

    private Template upsertTemplate(Long userId, Category category, RagDraftGenerateResultDTO.TemplateItem item) {
        Template template = templateRepository
                .findByUser_UserIdAndCategory_CategoryIdAndVariantLabel(userId, category.getCategoryId(), item.getVariantLabel())
                .map(existing -> {
                    existing.update(
                            item.getTitle(),
                            item.getVariantLabel(),
                            item.getSubjectTemplate(),
                            item.getBodyTemplate()
                    );
                    return existing;
                })
                .orElseGet(() -> Template.builder()
                        .userTemplateNo(templateNumberService.nextUserTemplateNo(userId))
                        .user(category.getUser())
                        .category(category)
                        .title(item.getTitle())
                        .variantLabel(item.getVariantLabel())
                        .subjectTemplate(item.getSubjectTemplate())
                        .bodyTemplate(item.getBodyTemplate())
                        .build());

        return templateRepository.save(template);
    }

    private void publishTemplateIndex(
            List<Template> templates,
            Category category,
            List<RagDraftGenerateResultDTO.TemplateItem> generatedItems
    ) {
        String requestId = "template-index-" + category.getCategoryId() + "-" + System.currentTimeMillis();
        String emailTone = profileRepository.findByUser_UserId(category.getUser().getUserId())
                .map(profile -> profile.getEmailTone() != null ? profile.getEmailTone().name() : null)
                .orElse(null);

        List<RagTemplateIndexRequestDTO.TemplateItem> indexItems = templates.stream()
                .map(template -> {
                    RagDraftGenerateResultDTO.TemplateItem generated = generatedItems.stream()
                            .filter(item -> item.getVariantLabel().equals(template.getVariantLabel()))
                            .findFirst()
                            .orElse(null);

                    String variantLabel = template.getVariantLabel() != null ? template.getVariantLabel() : "일반형";
                    String templatePurpose = generated != null ? generated.getTemplatePurpose() : null;
                    return RagTemplateIndexRequestDTO.TemplateItem.builder()
                            .templateId(template.getTemplateId())
                            .title(template.getTitle())
                            .categoryName(category.getCategoryName())
                            .emailTone(emailTone)
                            .metadata(
                                    RagTemplateIndexRequestDTO.Metadata.builder()
                                            .templatePurpose(templatePurpose)
                                            .searchSummary(variantLabel + " 템플릿")
                                            .semanticKeywords(List.of(category.getCategoryName(), variantLabel))
                                            .recommendedSituations(templatePurpose != null ? List.of(templatePurpose) : List.of())
                                            .build()
                            )
                            .build();
                })
                .toList();

        RagTemplateIndexRequestDTO message = RagTemplateIndexRequestDTO.builder()
                .requestId(requestId)
                .userId(category.getUser().getUserId())
                .payload(
                        RagTemplateIndexRequestDTO.Payload.builder()
                                .templates(indexItems)
                                .build()
                )
                .build();

        ragPublisher.publishTemplateIndex(message);
    }

    @Transactional
    public void handleTemplateMatched(RagTemplateMatchResultDTO result) {
        if (!"SUCCESS".equalsIgnoreCase(result.getStatus())) {
            String message = result.getError() != null ? result.getError().getMessage() : "RAG template match 실패";
            log.warn(
                    "[RagResultService] template match 실패 응답 수신 — requestId={}, jobId={}, message={}",
                    result.getRequestId(),
                    result.getJobId(),
                    message
            );
            return;
        }

        RagTemplateMatchResultDTO.Payload payload = result.getPayload();
        if (payload == null || payload.getEmailId() == null) {
            throw new IllegalArgumentException("RAG template match 결과 payload가 올바르지 않습니다.");
        }

        Long userId = result.getUserId();
        Long emailId = parseEmailId(payload.getEmailId());
        Email email = emailRepository.findByEmailIdAndUserId(emailId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("이메일을 찾을 수 없습니다: " + emailId));

        recommendationRepository.deleteByUser_UserIdAndEmail_EmailId(userId, emailId);

        List<RagTemplateMatchResultDTO.ResultItem> items = payload.getResults();
        if (items == null || items.isEmpty()) {
            log.info(
                    "[RagResultService] template match 결과가 비어있습니다 — userId={}, emailId={}",
                    userId,
                    emailId
            );
            pushTemplateMatchUpdate(userId, emailId, 0);
            return;
        }

        int rank = 1;
        for (RagTemplateMatchResultDTO.ResultItem item : items) {
            Template template = templateRepository.findById(item.getTemplateId())
                    .filter(found -> found.getUser().getUserId().equals(userId))
                    .orElseThrow(() -> new ResourceNotFoundException("추천 템플릿을 찾을 수 없습니다: " + item.getTemplateId()));

            EmailTemplateRecommendation recommendation = EmailTemplateRecommendation.builder()
                    .user(email.getUser())
                    .email(email)
                    .template(template)
                    .score(item.getScore())
                    .rankOrder(rank++)
                    .build();

            recommendationRepository.save(recommendation);
        }

        log.info(
                "[RagResultService] template match 결과 저장 완료 — userId={}, emailId={}, count={}",
                userId,
                emailId,
                items.size()
        );

        pushTemplateMatchUpdate(userId, emailId, items.size());
    }

    private Long parseEmailId(String rawEmailId) {
        try {
            return Long.parseLong(rawEmailId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("emailId는 숫자여야 합니다: " + rawEmailId, e);
        }
    }

    private void pushTemplateMatchUpdate(Long userId, Long emailId, int recommendationCount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email_id", emailId);
        payload.put("recommendation_count", recommendationCount);

        eventPublisher.publishEvent(new SseEvent(this, userId, "template-match-updated", payload));
    }
}
