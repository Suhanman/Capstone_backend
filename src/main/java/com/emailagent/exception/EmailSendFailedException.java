package com.emailagent.exception;

/**
 * Gmail API를 통한 메일 발송 실패 시 발생하는 예외.
 * GlobalExceptionHandler에서 HTTP 502 Bad Gateway로 처리된다.
 */
public class EmailSendFailedException extends RuntimeException {

    public EmailSendFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
