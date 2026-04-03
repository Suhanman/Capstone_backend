package com.emailagent.service;

import com.emailagent.domain.entity.AutomationRule;
import com.emailagent.domain.entity.Category;
import com.emailagent.domain.entity.Template;
import com.emailagent.domain.entity.User;
import com.emailagent.dto.request.automation.AutomationRuleRequest;
import com.emailagent.dto.request.automation.AutomationToggleRequest;
import com.emailagent.dto.response.automation.AutomationRuleResponse;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.repository.AutomationRuleRepository;
import com.emailagent.repository.CategoryRepository;
import com.emailagent.repository.TemplateRepository;
import com.emailagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AutomationService {

    private final AutomationRuleRepository automationRuleRepository;
    private final CategoryRepository categoryRepository;
    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;

    // =============================================
    // GET /api/automations/rules
    // 자동화 규칙 전체 목록 조회
    // =============================================
    @Transactional(readOnly = true)
    public AutomationRuleResponse.ListResponse getRules(Long userId) {
        List<AutomationRuleResponse> data = automationRuleRepository
                .findByUserIdWithDetails(userId)
                .stream()
                .map(AutomationRuleResponse::from)
                .toList();

        return AutomationRuleResponse.ListResponse.builder()
                .rules(data)
                .build();
    }

    // =============================================
    // POST /api/automations/rules
    // 새 규칙 추가
    // category_name이 없으면 INSERT, 있으면 재사용
    // =============================================
    @Transactional
    public AutomationRuleResponse createRule(Long userId, AutomationRuleRequest request) {
        User user = findUser(userId);

        // category_name 기준으로 찾거나 새로 생성
        Category category = findOrCreateCategory(user, request.getCategoryName(), request.getColor());

        // template_id가 있는 경우 조회 및 소유권 검증
        Template template = resolveTemplate(request.getTemplateId(), userId);

        AutomationRule rule = AutomationRule.builder()
                .user(user)
                .category(category)
                .template(template)
                .autoSendEnabled(request.isAutoSendEnabled())
                .build();

        return AutomationRuleResponse.from(automationRuleRepository.save(rule));
    }

    // =============================================
    // PUT /api/automations/rules/{rule_id}
    // 규칙 수정
    // =============================================
    @Transactional
    public AutomationRuleResponse updateRule(Long userId, Long ruleId, AutomationRuleRequest request) {
        AutomationRule rule = findRuleForUser(ruleId, userId);
        User user = rule.getUser();

        Category category = findOrCreateCategory(user, request.getCategoryName(), request.getColor());
        Template template = resolveTemplate(request.getTemplateId(), userId);

        rule.update(category, template, request.isAutoSendEnabled());

        return AutomationRuleResponse.from(rule);
    }

    // =============================================
    // DELETE /api/automations/rules/{rule_id}
    // 규칙 삭제 (Categories 테이블은 건드리지 않음)
    // =============================================
    @Transactional
    public void deleteRule(Long userId, Long ruleId) {
        AutomationRule rule = findRuleForUser(ruleId, userId);
        automationRuleRepository.delete(rule);
    }

    // =============================================
    // PATCH /api/automations/rules/{rule_id}/auto-send
    // 자동 발송 토글
    // =============================================
    @Transactional
    public AutomationRuleResponse toggleAutoSend(Long userId, Long ruleId, AutomationToggleRequest request) {
        if (request.getAutoSendEnabled() == null) {
            throw new IllegalArgumentException("auto_send_enabled 값이 필요합니다.");
        }
        AutomationRule rule = findRuleForUser(ruleId, userId);
        rule.toggleAutoSend(request.getAutoSendEnabled());
        return AutomationRuleResponse.from(rule);
    }

    // =============================================
    // PATCH /api/automations/rules/{rule_id}/auto-calendar
    // 일정 자동 등록 토글
    // =============================================
    @Transactional
    public AutomationRuleResponse toggleAutoCalendar(Long userId, Long ruleId, AutomationToggleRequest request) {
        if (request.getAutoCalendarEnabled() == null) {
            throw new IllegalArgumentException("auto_calendar_enabled 값이 필요합니다.");
        }
        AutomationRule rule = findRuleForUser(ruleId, userId);
        rule.toggleAutoCalendar(request.getAutoCalendarEnabled());
        return AutomationRuleResponse.from(rule);
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────────────

    /**
     * category_name이 해당 사용자에게 이미 존재하면 재사용, 없으면 새로 생성
     */
    private Category findOrCreateCategory(User user, String categoryName, String color) {
        return categoryRepository
                .findByUser_UserIdAndCategoryName(user.getUserId(), categoryName)
                .orElseGet(() -> categoryRepository.save(
                        Category.builder()
                                .user(user)
                                .categoryName(categoryName)
                                .color(color)
                                .build()
                ));
    }

    /**
     * templateId가 null이면 null 반환, 있으면 소유권 검증 후 반환
     */
    private Template resolveTemplate(Long templateId, Long userId) {
        if (templateId == null) {
            return null;
        }
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("템플릿을 찾을 수 없습니다."));
        if (!template.getUser().getUserId().equals(userId)) {
            throw new ResourceNotFoundException("템플릿을 찾을 수 없습니다.");
        }
        return template;
    }

    private AutomationRule findRuleForUser(Long ruleId, Long userId) {
        return automationRuleRepository
                .findByRuleIdAndUser_UserId(ruleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("자동화 규칙을 찾을 수 없습니다."));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
