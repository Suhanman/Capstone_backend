package com.emailagent.service;

import com.emailagent.domain.entity.*;
import com.emailagent.dto.request.business.*;
import com.emailagent.dto.response.business.*;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessProfileRepository profileRepository;
    private final BusinessResourceRepository resourceRepository;
    private final BusinessFaqRepository faqRepository;
    private final CategoryRepository categoryRepository;
    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final BusinessFileStorageService fileStorageService;

    // =============================================
    // 비즈니스 프로필 (Upsert)
    // =============================================

    @Transactional(readOnly = true)
    public BusinessProfileResponse getProfile(Long userId) {
        return profileRepository.findByUser_UserId(userId)
                .map(BusinessProfileResponse::from)
                .orElse(null);
    }

    @Transactional
    public ProfileSaveResponse upsertProfile(Long userId, BusinessProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        BusinessProfile profile = profileRepository.findByUser_UserId(userId)
                .orElse(BusinessProfile.builder().user(user).build());

        profile.update(request.getIndustryType(), request.getEmailTone(), request.getCompanyDescription());
        BusinessProfile saved = profileRepository.save(profile);

        return ProfileSaveResponse.builder()
                .profileId(saved.getProfileId())
                .build();
    }

    // =============================================
    // 비즈니스 파일 (Resources)
    // =============================================

    @Transactional(readOnly = true)
    public BusinessResourceListResponse getFiles(Long userId) {
        return BusinessResourceListResponse.builder()
                .resources(resourceRepository.findByUser_UserId(userId)
                        .stream()
                        .map(BusinessResourceResponse::from)
                        .toList())
                .build();
    }

    @Transactional
    public BusinessResourceResponse uploadFile(Long userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        String originalFileName = file.getOriginalFilename();
        String storedFilePath = fileStorageService.store(userId, file);

        String fileType = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileType = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toUpperCase();
        }

        BusinessResource resource = BusinessResource.builder()
                .user(user)
                .title(originalFileName)
                .fileName(originalFileName)
                .filePath(storedFilePath)
                .fileType(fileType)
                .build();

        return BusinessResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    public void deleteFile(Long userId, Long resourceId) {
        BusinessResource resource = resourceRepository
                .findByResourceIdAndUser_UserId(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("파일을 찾을 수 없습니다."));

        fileStorageService.delete(resource);

        resourceRepository.delete(resource);
    }

    // =============================================
    // FAQ
    // =============================================

    @Transactional(readOnly = true)
    public FaqListResponse getFaqs(Long userId) {
        return FaqListResponse.builder()
                .faqs(faqRepository.findByUser_UserId(userId)
                        .stream()
                        .map(FaqResponse::from)
                        .toList())
                .build();
    }

    @Transactional
    public FaqResponse createFaq(Long userId, FaqRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        BusinessFaq faq = BusinessFaq.builder()
                .user(user)
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .build();

        return FaqResponse.from(faqRepository.save(faq));
    }

    @Transactional
    public FaqResponse updateFaq(Long userId, Long faqId, FaqRequest request) {
        BusinessFaq faq = faqRepository.findByFaqIdAndUser_UserId(faqId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ를 찾을 수 없습니다."));

        faq.update(request.getQuestion(), request.getAnswer());
        return FaqResponse.from(faq);
    }

    @Transactional
    public void deleteFaq(Long userId, Long faqId) {
        BusinessFaq faq = faqRepository.findByFaqIdAndUser_UserId(faqId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ를 찾을 수 없습니다."));
        faqRepository.delete(faq);
    }

    // =============================================
    // 카테고리
    // =============================================

    @Transactional(readOnly = true)
    public CategoryListResponse getCategories(Long userId) {
        return CategoryListResponse.builder()
                .categories(categoryRepository.findByUser_UserId(userId)
                        .stream()
                        .map(CategoryResponse::from)
                        .toList())
                .build();
    }

    @Transactional
    public CategoryResponse createCategory(Long userId, CategoryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        if (categoryRepository.existsByUser_UserIdAndCategoryName(userId, request.getCategoryName())) {
            throw new IllegalArgumentException("이미 존재하는 카테고리입니다: " + request.getCategoryName());
        }

        Category category = Category.builder()
                .user(user)
                .categoryName(request.getCategoryName())
                .color(request.getColor())
                .keywords(request.getKeywords())
                .build();

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .filter(c -> c.getUser().getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("카테고리를 찾을 수 없습니다."));
        categoryRepository.delete(category);
    }

    // =============================================
    // RAG Context 생성
    // =============================================

    /**
     * userId의 비즈니스 프로필(회사 소개)과 FAQ를 조합하여 RAG context 문자열 반환.
     * 초안 생성/재생성 요청 시 AI 서버에 전달하는 컨텍스트로 활용된다.
     */
    @Transactional(readOnly = true)
    public String buildRagContext(Long userId) {
        StringBuilder sb = new StringBuilder();

        // 회사 소개 추가
        profileRepository.findByUser_UserId(userId).ifPresent(profile -> {
            if (profile.getCompanyDescription() != null && !profile.getCompanyDescription().isBlank()) {
                sb.append("[회사 소개]\n").append(profile.getCompanyDescription()).append("\n\n");
            }
        });

        // FAQ 목록 추가
        List<BusinessFaq> faqs = faqRepository.findByUser_UserId(userId);
        if (!faqs.isEmpty()) {
            sb.append("[자주 묻는 질문]\n");
            faqs.forEach(faq ->
                    sb.append("Q: ").append(faq.getQuestion())
                      .append("\nA: ").append(faq.getAnswer()).append("\n"));
        }

        return sb.toString().trim();
    }

    // =============================================
    // 템플릿 재생성
    // =============================================

    @Transactional(readOnly = true)
    public TemplateRegenerateResponse regenerateTemplates(Long userId, TemplateRegenerateRequest request) {
        int processingCount;

        if (request.isRegenerateAll()) {
            processingCount = templateRepository.findByUser_UserId(userId).size();
        } else {
            List<Long> templateIds = request.getTemplateIds();
            if (templateIds == null || templateIds.isEmpty()) {
                throw new IllegalArgumentException("재생성할 템플릿 ID를 지정해주세요.");
            }
            processingCount = (int) templateIds.stream()
                    .filter(id -> templateRepository.findById(id)
                            .map(t -> t.getUser().getUserId().equals(userId))
                            .orElse(false))
                    .count();
        }

        if (processingCount == 0) {
            throw new IllegalArgumentException("재생성 가능한 템플릿이 없습니다.");
        }

        log.info("템플릿 재생성 요청: userId={}, count={}", userId, processingCount);
        return TemplateRegenerateResponse.of(processingCount);
    }
}
