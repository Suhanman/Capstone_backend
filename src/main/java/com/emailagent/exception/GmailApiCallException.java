package com.emailagent.exception;

public class GmailApiCallException extends RuntimeException {

    public GmailApiCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
