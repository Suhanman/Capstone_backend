package com.emailagent.service;

import com.emailagent.domain.entity.Category;
import com.emailagent.domain.entity.Template;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.rabbitmq.dto.RagDraftGenerateResultDTO;
import com.emailagent.rabbitmq.dto.RagTemplateIndexRequestDTO;
import com.emailagent.rabbitmq.publisher.RagPublisher;
import com.emailagent.repository.BusinessProfileRepository;
import com.emailagent.repository.CategoryRepository;
import com.emailagent.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
    private final RagPublisher ragPublisher;

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

        Template template = templateRepository.findBestMatchingTemplate(userId, categoryId)
                .map(existing -> {
                    existing.update(
                            payload.getTitle(),
                            payload.getSubjectTemplate(),
                            payload.getBodyTemplate()
                    );
                    return existing;
                })
                .orElseGet(() -> Template.builder()
                        .user(category.getUser())
                        .category(category)
                        .title(payload.getTitle())
                        .subjectTemplate(payload.getSubjectTemplate())
                        .bodyTemplate(payload.getBodyTemplate())
                        .build());

        Template saved = templateRepository.save(template);
        publishTemplateIndex(saved, category);

        log.info(
                "[RagResultService] draft 결과로 템플릿 저장 완료 — userId={}, categoryId={}, templateId={}",
                userId,
                categoryId,
                saved.getTemplateId()
        );
    }

    private void publishTemplateIndex(Template template, Category category) {
        String requestId = "template-index-" + template.getTemplateId();
        String emailTone = profileRepository.findByUser_UserId(category.getUser().getUserId())
                .map(profile -> profile.getEmailTone() != null ? profile.getEmailTone().name() : null)
                .orElse(null);

        RagTemplateIndexRequestDTO message = RagTemplateIndexRequestDTO.builder()
                .requestId(requestId)
                .userId(category.getUser().getUserId())
                .payload(
                        RagTemplateIndexRequestDTO.Payload.builder()
                                .templates(List.of(
                                        RagTemplateIndexRequestDTO.TemplateItem.builder()
                                                .templateId(template.getTemplateId())
                                                .title(template.getTitle())
                                                .categoryName(category.getCategoryName())
                                                .emailTone(emailTone)
                                                .build()
                                ))
                                .build()
                )
                .build();

        ragPublisher.publishTemplateIndex(message);
    }
}
