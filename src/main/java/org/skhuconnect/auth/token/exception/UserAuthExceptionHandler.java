package org.skhuconnect.auth.token.exception;

import org.skhuconnect.auth.token.controller.UserAuthController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = UserAuthController.class)
public class UserAuthExceptionHandler {

    @ExceptionHandler(UserAuthException.class)
    public ProblemDetail handle(UserAuthException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case INVALID_CREDENTIALS, TOKEN_INVALID -> HttpStatus.UNAUTHORIZED;
            case TOKEN_EXPIRED -> HttpStatus.GONE;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(switch (exception.getReason()) {
            case INVALID_CREDENTIALS -> "Invalid credentials";
            case TOKEN_INVALID -> "Invalid refresh token";
            case TOKEN_EXPIRED -> "Refresh token expired";
        });
        return detail;
    }
}
