package com.emailagent.dto.response.inbox;

import com.emailagent.dto.inbox.AttachmentMetaDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 프론트엔드 전달용 첨부파일 응답 DTO.
 * 내부 통신용 gmail_attachment_id는 보안상 절대 노출하지 않는다.
 */
@Getter
@Builder
public class AttachmentResponseDto {

    @JsonProperty("attachment_id")
    private int attachmentId;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("content_type")
    private String contentType;

    private Long size;

    public static AttachmentResponseDto from(AttachmentMetaDto meta) {
        return AttachmentResponseDto.builder()
                .attachmentId(meta.getAttachmentId())
                .fileName(meta.getFileName())
                .contentType(meta.getContentType())
                .size(meta.getSize())
                .build();
    }
}
