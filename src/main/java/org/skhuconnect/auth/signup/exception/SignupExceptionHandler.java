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
            case LOGIN_ID_ALREADY_EXISTS, EMAIL_ALREADY_EXISTS, REJOIN_RESTRICTED ->
                    HttpStatus.CONFLICT;
            case DEPARTMENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_ACCOUNT_REQUEST, TERMS_NOT_AGREED, UNSUPPORTED_TERMS_VERSION ->
                    HttpStatus.BAD_REQUEST;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(switch (exception.getReason()) {
            case INVALID_ACCOUNT_REQUEST -> "Invalid signup account policy";
            case LOGIN_ID_ALREADY_EXISTS -> "Login ID already exists";
            case EMAIL_ALREADY_EXISTS -> "Email already exists";
            case REJOIN_RESTRICTED ->
                    "Re-registration is available 30 days after withdrawal";
            case DEPARTMENT_NOT_FOUND -> "Department not found";
            case TERMS_NOT_AGREED -> "필수 이용약관 동의가 필요합니다";
            case UNSUPPORTED_TERMS_VERSION -> "지원하지 않는 이용약관 버전입니다";
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
