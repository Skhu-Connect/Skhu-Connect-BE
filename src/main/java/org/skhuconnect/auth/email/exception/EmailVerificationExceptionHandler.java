package org.skhuconnect.auth.email.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes =
        org.skhuconnect.auth.email.controller.EmailVerificationController.class)
public class EmailVerificationExceptionHandler {

    @ExceptionHandler(EmailVerificationException.class)
    public ProblemDetail handleVerification(EmailVerificationException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case EMAIL_ALREADY_REGISTERED -> HttpStatus.CONFLICT;
            case RESEND_TOO_SOON -> HttpStatus.TOO_MANY_REQUESTS;
            case CODE_EXPIRED, TOKEN_EXPIRED -> HttpStatus.GONE;
            default -> HttpStatus.BAD_REQUEST;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle("Email verification request failed");
        return detail;
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ProblemDetail handleDelivery() {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        detail.setTitle("Email delivery failed");
        return detail;
    }
}
