package com.emailagent.service;

import com.emailagent.domain.entity.Category;
import com.emailagent.dto.request.onboarding.InitialTemplateGenerateRequest;
import com.emailagent.dto.response.onboarding.InitialTemplateGenerateResponse;
import com.emailagent.dto.response.onboarding.OnboardingStatusResponse;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.rabbitmq.dto.RagDraftGenerateRequestDTO;
import com.emailagent.rabbitmq.dto.RagKnowledgeIngestRequestDTO;
import com.emailagent.rabbitmq.publisher.RagPublisher;
import com.emailagent.domain.entity.BusinessFaq;
import com.emailagent.domain.entity.BusinessResource;
import com.emailagent.repository.CategoryRepository;
import com.emailagent.repository.BusinessFaqRepository;
import com.emailagent.repository.BusinessResourceRepository;
import com.emailagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BusinessFaqRepository faqRepository;
    private final BusinessResourceRepository resourceRepository;
    private final BusinessService businessService;
    private final RagPublisher ragPublisher;

    // =============================================
    // GET /api/onboarding/status
    // =============================================

    @Transactional(readOnly = true)
    public OnboardingStatusResponse getOnboardingStatus(Long userId) {
        boolean completed = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."))
                .isOnboardingCompleted();
        return OnboardingStatusResponse.of(completed);
    }

    // =============================================
    // POST /api/onboarding/complete
    // =============================================

    @Transactional
    public void completeOnboarding(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."))
                .completeOnboarding();
    }

    // =============================================
    // POST /api/business/templates/generate-initial
    // =============================================

    @Transactional
    public InitialTemplateGenerateResponse generateInitialTemplates(Long userId, InitialTemplateGenerateRequest request) {
        List<Long> categoryIds = request.getCategoryIds();
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new IllegalArgumentException("category_ids는 필수입니다.");
        }

        // 1. category_ids 기준으로 카테고리 조회 (본인 소유 검증)
        List<Category> categories = categoryIds.stream()
                .map(id -> categoryRepository.findById(id)
                        .filter(c -> c.getUser().getUserId().equals(userId))
                        .orElseThrow(() -> new ResourceNotFoundException("카테고리를 찾을 수 없습니다: " + id)))
                .toList();

        // 2. 지식 적재용 FAQ / PDF 수집
        List<BusinessFaq> selectedFaqs = resolveSelectedFaqs(userId, request.getFaqIds());
        List<BusinessResource> selectedResources = resolveSelectedResources(userId, request.getResourceIds());

        // 3. 선택된 FAQ / PDF를 knowledge ingest 큐에 발행
        String knowledgeJobId = publishKnowledgeIngest(userId, selectedFaqs, selectedResources);

        // 4. RAG context 생성
        String ragContext = businessService.buildRagContext(userId);

        // 5. 카테고리별로 RAG draft 큐에 템플릿 생성 요청 발행 (mode="generate")
        List<String> draftJobIds = categories.stream()
                .map(category -> {
                    String requestId = UUID.randomUUID().toString();
                    String jobId = "rag-draft-" + userId + "-" + category.getCategoryId() + "-" + requestId;

                    RagDraftGenerateRequestDTO message = RagDraftGenerateRequestDTO.builder()
                            .jobId(jobId)
                            .requestId(requestId)
                            .userId(userId)
                            .mode("generate")
                            .categoryId(category.getCategoryId())
                            .categoryName(category.getCategoryName())
                            .industryType(request.getIndustryType())
                            .emailTone(request.getEmailTone())
                            .companyDescription(request.getCompanyDescription())
                            .ragContext(ragContext)
                            .templateCount(1)
                            .build();

                    ragPublisher.publishDraftGeneration(message);

                    log.info(
                            "[OnboardingService] RAG 초기 템플릿 생성 요청 발행 — userId={}, categoryId={}, categoryName={}, jobId={}, requestId={}",
                            userId,
                            category.getCategoryId(),
                            category.getCategoryName(),
                            jobId,
                            requestId
                    );
                    return jobId;
                })
                .toList();

        // 6. processing_count = category_ids 개수 반환
        return InitialTemplateGenerateResponse.of(categories.size(), draftJobIds, knowledgeJobId);
    }

    private List<BusinessFaq> resolveSelectedFaqs(Long userId, List<Long> faqIds) {
        if (faqIds == null || faqIds.isEmpty()) {
            return List.of();
        }

        return faqIds.stream()
                .map(faqId -> faqRepository.findByFaqIdAndUser_UserId(faqId, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("FAQ를 찾을 수 없습니다: " + faqId)))
                .toList();
    }

    private List<BusinessResource> resolveSelectedResources(Long userId, List<Long> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return List.of();
        }

        return resourceIds.stream()
                .map(resourceId -> resourceRepository.findByResourceIdAndUser_UserId(resourceId, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("파일을 찾을 수 없습니다: " + resourceId)))
                .toList();
    }

    private String publishKnowledgeIngest(
            Long userId,
            List<BusinessFaq> faqs,
            List<BusinessResource> resources
    ) {
        if (faqs.isEmpty() && resources.isEmpty()) {
            log.info("[OnboardingService] 선택된 FAQ/PDF가 없어 knowledge ingest 발행을 생략합니다. userId={}", userId);
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
}
