package com.emailagent.exception;
public class EmailNotFoundException extends RuntimeException {
    public EmailNotFoundException(Long emailId) {
        super("이메일을 찾을 수 없습니다: " + emailId);
    }
}
