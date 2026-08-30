package org.skhuconnect.auth.token.exception;

import org.skhuconnect.auth.email.exception.EmailVerificationException;
import org.skhuconnect.auth.loginid.controller.LoginIdFindController;
import org.skhuconnect.auth.token.controller.UserAuthController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {
        UserAuthController.class,
        LoginIdFindController.class
})
public class UserAuthExceptionHandler {

    @ExceptionHandler(UserAuthException.class)
    public ProblemDetail handle(UserAuthException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case INVALID_CREDENTIALS, TOKEN_INVALID -> HttpStatus.UNAUTHORIZED;
            case TOKEN_EXPIRED -> HttpStatus.GONE;
            case ACCOUNT_BANNED -> HttpStatus.FORBIDDEN;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(switch (exception.getReason()) {
            case INVALID_CREDENTIALS -> "Invalid credentials";
            case TOKEN_INVALID -> "Invalid refresh token";
            case TOKEN_EXPIRED -> "Refresh token expired";
            case ACCOUNT_BANNED -> "Account banned";
        });
        if (exception.getDetail() != null) {
            detail.setDetail(exception.getDetail());
        }
        return detail;
    }

    @ExceptionHandler(EmailVerificationException.class)
    public ProblemDetail handleVerificationToken(EmailVerificationException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case TOKEN_EXPIRED -> HttpStatus.GONE;
            case TOKEN_USED -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle("Invalid verification token");
        return detail;
    }
}
