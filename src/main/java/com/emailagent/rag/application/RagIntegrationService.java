package com.emailagent.rag.application;

import com.emailagent.domain.entity.BusinessFaq;
import com.emailagent.domain.entity.BusinessResource;
import com.emailagent.domain.entity.Category;
import com.emailagent.domain.entity.Email;
import com.emailagent.domain.entity.EmailAnalysisResult;
import com.emailagent.dto.request.onboarding.InitialTemplateGenerateRequest;
import com.emailagent.rabbitmq.dto.RagDraftGenerateRequestDTO;
import com.emailagent.rabbitmq.dto.RagKnowledgeIngestRequestDTO;
import com.emailagent.rabbitmq.dto.RagTemplateMatchRequestDTO;
import com.emailagent.rabbitmq.publisher.RagPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Backend domain services should depend on this facade instead of RabbitMQ DTOs directly.
 * That keeps future RAG contract changes inside the RAG integration boundary.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagIntegrationService {

    private final RagPublisher ragPublisher;

    public String requestKnowledgeIngest(
            Long userId,
            List<BusinessFaq> faqs,
            List<BusinessResource> resources
    ) {
        if (faqs.isEmpty() && resources.isEmpty()) {
            log.info("[RagIntegrationService] 선택된 FAQ/PDF가 없어 knowledge ingest 발행을 생략합니다. userId={}", userId);
            return null;
        }

        String requestId = UUID.randomUUID().toString();
        String jobId = "rag-ingest-" + userId + "-" + requestId;

        RagKnowledgeIngestRequestDTO message = RagKnowledgeIngestRequestDTO.builder()
                .jobId(jobId)
                .requestId(requestId)
                .userId(userId)
                .payload(
                        RagKnowledgeIngestRequestDTO.Payload.builder()
                                .faqs(faqs.stream()
                                        .map(faq -> RagKnowledgeIngestRequestDTO.FaqItem.builder()
                                                .sourceId("faq-" + faq.getFaqId())
                                                .question(faq.getQuestion())
                                                .answer(faq.getAnswer())
                                                .build())
                                        .toList())
                                .manuals(resources.stream()
                                        .map(resource -> RagKnowledgeIngestRequestDTO.ManualItem.builder()
                                                .sourceId("manual-" + resource.getResourceId())
                                                .title(resource.getTitle())
                                                .fileName(resource.getFileName())
                                                .localPath(resource.getFilePath())
                                                .build())
                                        .toList())
                                .build()
                )
                .build();

        ragPublisher.publishKnowledgeIngest(message);
        return jobId;
    }

    public List<String> requestInitialTemplateDrafts(
            Long userId,
            List<Category> categories,
            InitialTemplateGenerateRequest request,
            String ragContext
    ) {
        return categories.stream()
                .map(category -> requestInitialTemplateDraft(userId, category, request, ragContext))
                .toList();
    }

    public void requestTemplateMatch(Email email, EmailAnalysisResult analysisResult) {
        if (analysisResult.getIntent() == null || analysisResult.getSummaryText() == null) {
            log.debug(
                    "[RagIntegrationService] templates.match 발행 생략 — emailId={}, intent={}, summary={}",
                    email.getEmailId(),
                    analysisResult.getIntent(),
                    analysisResult.getSummaryText()
            );
            return;
        }

        String requestId = "template-match-" + email.getEmailId();
        String jobId = "template-match-" + email.getEmailId();

        RagTemplateMatchRequestDTO payload = RagTemplateMatchRequestDTO.builder()
                .jobId(jobId)
                .requestId(requestId)
                .userId(email.getUser().getUserId())
                .payload(
                        RagTemplateMatchRequestDTO.Payload.builder()
                                .emailId(String.valueOf(email.getEmailId()))
                                .subject(email.getSubject())
                                .body(email.getBodyClean())
                                .summary(analysisResult.getSummaryText())
                                .intent(analysisResult.getIntent())
                                .domain(analysisResult.getDomain())
                                .topK(3)
                                .build()
                )
                .build();

        ragPublisher.publishTemplateMatch(payload);
    }

    private String requestInitialTemplateDraft(
            Long userId,
            Category category,
            InitialTemplateGenerateRequest request,
            String ragContext
    ) {
        String requestId = UUID.randomUUID().toString();
        String jobId = "rag-draft-" + userId + "-" + category.getCategoryId() + "-" + requestId;

        RagDraftGenerateRequestDTO message = RagDraftGenerateRequestDTO.builder()
                .jobId(jobId)
                .requestId(requestId)
                .userId(userId)
                .mode("generate")
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .categoryKeywords(category.getKeywords())
                .industryType(request.getIndustryType())
                .emailTone(request.getEmailTone())
                .companyDescription(request.getCompanyDescription())
                .ragContext(ragContext)
                .templateCount(3)
                .build();

        ragPublisher.publishDraftGeneration(message);

        log.info(
                "[RagIntegrationService] RAG 초기 템플릿 생성 요청 발행 — userId={}, categoryId={}, categoryName={}, jobId={}, requestId={}",
                userId,
                category.getCategoryId(),
                category.getCategoryName(),
                jobId,
                requestId
        );
        return jobId;
    }
}
