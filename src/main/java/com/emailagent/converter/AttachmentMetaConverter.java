package com.emailagent.converter;

import com.emailagent.dto.inbox.AttachmentMetaDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

/**
 * Emails.attachments_meta JSON 컬럼 ↔ List<AttachmentMetaDto> 변환 컨버터.
 * JPA는 JSON 컬럼을 기본으로 지원하지 않으므로 AttributeConverter로 처리한다.
 */
@Converter
public class AttachmentMetaConverter implements AttributeConverter<List<AttachmentMetaDto>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<AttachmentMetaDto> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("attachments_meta JSON 직렬화 실패", e);
        }
    }

    @Override
    public List<AttachmentMetaDto> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(dbData, new TypeReference<List<AttachmentMetaDto>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("attachments_meta JSON 역직렬화 실패", e);
        }
    }
}
