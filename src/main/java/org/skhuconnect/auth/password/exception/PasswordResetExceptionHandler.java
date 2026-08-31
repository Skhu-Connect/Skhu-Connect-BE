package org.skhuconnect.auth.password.exception;

import org.skhuconnect.auth.email.exception.EmailVerificationException;
import org.skhuconnect.auth.password.controller.PasswordResetController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PasswordResetController.class)
public class PasswordResetExceptionHandler {
    @ExceptionHandler(PasswordResetException.class)
    public ProblemDetail handlePasswordReset(PasswordResetException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case INVALID_PASSWORD -> HttpStatus.BAD_REQUEST;
            case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(exception.getReason() == PasswordResetException.Reason.INVALID_PASSWORD
                ? "Invalid password policy" : "User not found");
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
