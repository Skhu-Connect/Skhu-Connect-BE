package org.skhuconnect.auth.signup.exception;

import org.skhuconnect.auth.email.exception.EmailVerificationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes =
        org.skhuconnect.auth.signup.controller.SignupController.class)
public class SignupExceptionHandler {

    @ExceptionHandler(SignupException.class)
    public ProblemDetail handleSignup(SignupException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case LOGIN_ID_ALREADY_EXISTS, EMAIL_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case DEPARTMENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(switch (exception.getReason()) {
            case LOGIN_ID_ALREADY_EXISTS -> "Login ID already exists";
            case EMAIL_ALREADY_EXISTS -> "Email already exists";
            case DEPARTMENT_NOT_FOUND -> "Department not found";
        });
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