package com.emailagent.exception;
public class TemplateNotFoundException extends RuntimeException {
    public TemplateNotFoundException(Long templateId) {
        super("템플릿을 찾을 수 없습니다: " + templateId);
    }
}
