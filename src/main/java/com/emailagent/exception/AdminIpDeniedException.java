package com.emailagent.exception;

public class AdminIpDeniedException extends RuntimeException {

    public AdminIpDeniedException(String clientIp) {
        super("허용되지 않는 접근 IP입니다: " + clientIp);
    }
}
