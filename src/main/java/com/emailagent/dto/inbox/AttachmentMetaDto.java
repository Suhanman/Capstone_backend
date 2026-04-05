package com.emailagent.dto.inbox;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Emails.attachments_meta JSON 배열의 원소 스펙.
 * - attachment_id    : 우리 API Path Variable로 받을 1-based 시퀀스 번호 (int)
 * - gmail_attachment_id : Gmail API 요청용 실제 첨부파일 ID (String)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentMetaDto {

    @JsonProperty("attachment_id")
    private int attachmentId;

    @JsonProperty("gmail_attachment_id")
    private String gmailAttachmentId;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("content_type")
    private String contentType;

    @JsonProperty("size")
    private Long size;
}
