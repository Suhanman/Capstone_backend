package com.emailagent.service.admin;

import com.emailagent.domain.entity.AutomationRule;
import com.emailagent.domain.entity.Category;
import com.emailagent.domain.entity.Template;
import com.emailagent.domain.entity.User;
import com.emailagent.dto.request.admin.AdminAutomationRuleCreateRequest;
import com.emailagent.dto.request.admin.AdminAutomationRuleUpdateRequest;
import com.emailagent.dto.response.admin.AdminSimpleResponse;
import com.emailagent.dto.response.admin.automation.AdminAutomationRuleCreateResponse;
import com.emailagent.dto.response.admin.automation.AdminAutomationRuleDetailResponse;
import com.emailagent.dto.response.admin.automation.AdminAutomationRuleListResponse;
import com.emailagent.dto.response.admin.automation.AdminAutomationRuleUpdateResponse;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.repository.AutomationRuleRepository;
import com.emailagent.repository.CategoryRepository;
import com.emailagent.repository.TemplateRepository;
import com.emailagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAutomationService {

    private final AutomationRuleRepository automationRuleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TemplateRepository templateRepository;

    /**
     * 전체 자동화 규칙 목록 조회 (user_id 선택 필터, 페이징)
     */
    @Transactional(readOnly = true)
    public AdminAutomationRuleListResponse getRules(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size);

        Page<AutomationRule> resultPage = (userId != null)
                ? automationRuleRepository.findByUserId(userId, pageable)
                : automationRuleRepository.findAllRules(pageable);

        List<AdminAutomationRuleListResponse.RuleItem> items = resultPage.getContent().stream()
                .map(rule -> new AdminAutomationRuleListResponse.RuleItem(
                        rule.getRuleId(),
                        rule.getUser().getUserId(),
                        rule.isActive(),
                        rule.getName(),
                        rule.getCategory() != null ? rule.getCategory().getCategoryName() : null,
                        rule.getTriggerCondition(),
                        rule.getActionDescription(),
                        rule.getUpdatedAt()
                ))
                .collect(Collectors.toList());

        return new AdminAutomationRuleListResponse(resultPage.getTotalElements(), items);
    }

    /**
     * 특정 자동화 규칙 상세 조회
     */
    @Transactional(readOnly = true)
    public AdminAutomationRuleDetailResponse getRuleDetail(Long ruleId) {
        AutomationRule rule = automationRuleRepository.findDetailByRuleId(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("자동화 규칙을 찾을 수 없습니다. ruleId=" + ruleId));

        Long templateId = rule.getTemplate() != null ? rule.getTemplate().getTemplateId() : null;

        return new AdminAutomationRuleDetailResponse(
                rule.getRuleId(),
                rule.getUser().getUserId(),
                rule.getCategory().getCategoryId(),
                templateId,
                rule.isAutoSendEnabled(),
                rule.isAutoCalendarEnabled(),
                rule.isActive(),
                rule.getName(),
                rule.getCategory().getCategoryName(),
                rule.getTriggerCondition(),
                rule.getActionDescription(),
                rule.getUpdatedAt()
        );
    }

    /**
     * 자동화 규칙 생성 (관리자가 특정 사용자에게 규칙 할당)
     */
    @Transactional
    public AdminAutomationRuleCreateResponse createRule(AdminAutomationRuleCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. userId=" + request.getUserId()));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("카테고리를 찾을 수 없습니다. categoryId=" + request.getCategoryId()));

        Template template = null;
        if (request.getTemplateId() != null) {
            template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("템플릿을 찾을 수 없습니다. templateId=" + request.getTemplateId()));
        }

        AutomationRule rule = AutomationRule.builder()
                .user(user)
                .category(category)
                .template(template)
                .autoSendEnabled(request.getAutoSendEnabled())
                .autoCalendarEnabled(request.getAutoCalendarEnabled())
                .name(request.getName())
                .triggerCondition(request.getTriggerCondition())
                .actionDescription(request.getActionDescription())
                .build();

        AutomationRule saved = automationRuleRepository.save(rule);
        return new AdminAutomationRuleCreateResponse(saved.getRuleId());
    }

    /**
     * 자동화 규칙 수정 (관리자)
     * - null 필드는 변경하지 않음 (선택적 업데이트)
     */
    @Transactional
    public AdminAutomationRuleUpdateResponse updateRule(Long ruleId, AdminAutomationRuleUpdateRequest request) {
        AutomationRule rule = automationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("자동화 규칙을 찾을 수 없습니다. ruleId=" + ruleId));

        // template: templateId가 있으면 조회, 없으면 null 유지
        Template template = null;
        if (request.getTemplateId() != null) {
            template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("템플릿을 찾을 수 없습니다. templateId=" + request.getTemplateId()));
        }

        rule.updateByAdmin(template, request.getIsActive(), request.getAutoSendEnabled());
        rule.updateDetails(request.getName(), request.getTriggerCondition(), request.getActionDescription());

        return new AdminAutomationRuleUpdateResponse(rule.getRuleId());
    }

    /**
     * 자동화 규칙 삭제 (관리자)
     */
    @Transactional
    public AdminSimpleResponse deleteRule(Long ruleId) {
        if (!automationRuleRepository.existsById(ruleId)) {
            throw new ResourceNotFoundException("자동화 규칙을 찾을 수 없습니다. ruleId=" + ruleId);
        }
        automationRuleRepository.deleteById(ruleId);
        return AdminSimpleResponse.OK;
    }
}
