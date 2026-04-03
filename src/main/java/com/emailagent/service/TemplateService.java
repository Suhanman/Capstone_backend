package com.emailagent.service;

import com.emailagent.domain.entity.Category;
import com.emailagent.domain.entity.Template;
import com.emailagent.domain.entity.User;
import com.emailagent.dto.request.template.TemplateCreateRequest;
import com.emailagent.dto.request.template.TemplateUpdateRequest;
import com.emailagent.dto.response.template.TemplateListResponse;
import com.emailagent.dto.response.template.TemplateResponse;
import com.emailagent.exception.TemplateNotFoundException;
import com.emailagent.repository.CategoryRepository;
import com.emailagent.repository.TemplateRepository;
import com.emailagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public TemplateListResponse getTemplates(Long userId) {
        return TemplateListResponse.builder()
                .templates(templateRepository.findByUser_UserId(userId)
                        .stream()
                        .map(TemplateResponse::from)
                        .toList())
                .build();
    }

    @Transactional
    public TemplateResponse createTemplate(Long userId, TemplateCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("카테고리 없음"));

        Template template = Template.builder()
                .user(user)
                .category(category)
                .title(request.getTitle())
                .subjectTemplate(request.getSubjectTemplate())
                .bodyTemplate(request.getBodyTemplate())
                .build();

        return TemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public TemplateResponse updateTemplate(Long templateId, Long userId, TemplateUpdateRequest request) {
        Template template = templateRepository.findById(templateId)
                .filter(t -> t.getUser().getUserId().equals(userId))
                .orElseThrow(() -> new TemplateNotFoundException(templateId));

        template.update(request.getTitle(), request.getSubjectTemplate(), request.getBodyTemplate());
        return TemplateResponse.from(template);
    }

    @Transactional
    public void deleteTemplate(Long templateId, Long userId) {
        Template template = templateRepository.findById(templateId)
                .filter(t -> t.getUser().getUserId().equals(userId))
                .orElseThrow(() -> new TemplateNotFoundException(templateId));
        templateRepository.delete(template);
    }

    /**
     * 템플릿 변수 치환
     * 예: "안녕하세요 {{고객명}}님" + {고객명: "홍길동"} → "안녕하세요 홍길동님"
     */
    public String fillTemplate(String templateBody, Map<String, Object> variables) {
        StringBuffer result = new StringBuffer();
        Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(templateBody);

        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = variables.getOrDefault(key, "{{" + key + "}}").toString();
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
