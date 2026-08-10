package org.skhuconnect.admin.token.exception;

import org.skhuconnect.admin.token.controller.AdminAuthController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdminAuthController.class)
public class AdminAuthExceptionHandler {

    @ExceptionHandler(AdminAuthException.class)
    public ProblemDetail handle(AdminAuthException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case INVALID_CREDENTIALS, TOKEN_INVALID -> HttpStatus.UNAUTHORIZED;
            case TOKEN_EXPIRED -> HttpStatus.GONE;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(switch (exception.getReason()) {
            case INVALID_CREDENTIALS -> "인증 정보가 올바르지 않습니다";
            case TOKEN_INVALID -> "리프레시 토큰이 올바르지 않습니다";
            case TOKEN_EXPIRED -> "Refresh token expired";
        });
        return detail;
    }
}