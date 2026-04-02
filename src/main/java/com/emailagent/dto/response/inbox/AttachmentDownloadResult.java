package com.emailagent.dto.response.inbox;

/**
 * Gmail API에서 받은 첨부파일 바이트 데이터와 DB 메타데이터를 함께 Controller에 전달하기 위한 레코드.
 * 성공 응답은 JSON이 아닌 바이너리 스트림이므로 BaseResponse를 상속하지 않는다.
 */
public record AttachmentDownloadResult(byte[] data, String fileName, String mimeType) {}
