package com.emailagent.exception;

import com.emailagent.dto.response.auth.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailNotFoundException.class)
    public ResponseEntity<BaseResponse> handleEmailNotFound(EmailNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new BaseResponse(HttpStatus.NOT_FOUND.value(), e.getMessage()));
    }

    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<BaseResponse> handleTemplateNotFound(TemplateNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new BaseResponse(HttpStatus.NOT_FOUND.value(), e.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse> handleResourceNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new BaseResponse(HttpStatus.NOT_FOUND.value(), e.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<BaseResponse> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new BaseResponse(HttpStatus.UNAUTHORIZED.value(), e.getMessage()));
    }

    @ExceptionHandler(InsufficientScopeException.class)
    public ResponseEntity<BaseResponse> handleInsufficientScope(InsufficientScopeException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new BaseResponse(HttpStatus.FORBIDDEN.value(), e.getMessage()));
    }

    @ExceptionHandler(CalendarNotConnectedException.class)
    public ResponseEntity<BaseResponse> handleCalendarNotConnected(CalendarNotConnectedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new BaseResponse(HttpStatus.FORBIDDEN.value(), e.getMessage()));
    }

    @ExceptionHandler(EmailSendFailedException.class)
    public ResponseEntity<BaseResponse> handleEmailSendFailed(EmailSendFailedException e) {
        log.error("Gmail 메일 발송 실패", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new BaseResponse(HttpStatus.BAD_GATEWAY.value(), e.getMessage()));
    }

    @ExceptionHandler(GmailApiCallException.class)
    public ResponseEntity<BaseResponse> handleGmailApiCall(GmailApiCallException e) {
        log.error("Gmail API 호출 실패", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new BaseResponse(HttpStatus.BAD_GATEWAY.value(), e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<BaseResponse> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new BaseResponse(HttpStatus.CONFLICT.value(), e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new BaseResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<BaseResponse> handleMail(MailException e) {
        log.error("이메일 발송 실패", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new BaseResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), "이메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요."));
    }

    // Validation 오류 (@Valid) — 필드별 오류를 result_req에 합쳐서 반환
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse> handleValidation(MethodArgumentNotValidException e) {
        String resultReq = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new BaseResponse(HttpStatus.BAD_REQUEST.value(),
                        "입력값이 올바르지 않습니다: " + resultReq));
    }

    // Google API 네트워크 오류 (token exchange, API 호출 실패 등) → 502
    @ExceptionHandler(IOException.class)
    public ResponseEntity<BaseResponse> handleIo(IOException e) {
        log.error("Google API 통신 오류", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new BaseResponse(HttpStatus.BAD_GATEWAY.value(), "외부 API 통신에 실패했습니다. 잠시 후 다시 시도해 주세요."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse> handleGeneral(Exception e) {
        log.error("예상치 못한 오류 발생", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BaseResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 오류가 발생했습니다."));
    }
}
