package com.emailagent.service;

import com.emailagent.domain.entity.Category;
import com.emailagent.dto.request.onboarding.InitialTemplateGenerateRequest;
import com.emailagent.dto.response.onboarding.InitialTemplateGenerateResponse;
import com.emailagent.dto.response.onboarding.OnboardingStatusResponse;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.rabbitmq.config.RabbitMQConfig;
import com.emailagent.rabbitmq.dto.RagDraftGenerateRequestDTO;
import com.emailagent.repository.CategoryRepository;
import com.emailagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private final BusinessService businessService;
    private final RabbitTemplate rabbitTemplate;

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

    @Transactional(readOnly = true)
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

        // 2. RAG context 생성
        String ragContext = businessService.buildRagContext(userId);

        // 3. 카테고리별로 RAG draft 큐에 템플릿 생성 요청 발행 (mode="generate")
        for (Category category : categories) {
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

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_APP2RAG,
                    RabbitMQConfig.RK_RAG_DRAFT_INBOUND,
                    message
            );

            log.info(
                    "[OnboardingService] RAG 초기 템플릿 생성 요청 발행 — userId={}, categoryId={}, categoryName={}, jobId={}, requestId={}",
                    userId,
                    category.getCategoryId(),
                    category.getCategoryName(),
                    jobId,
                    requestId
            );
        }

        // 4. processing_count = category_ids 개수 반환
        return InitialTemplateGenerateResponse.of(categories.size());
    }
}
