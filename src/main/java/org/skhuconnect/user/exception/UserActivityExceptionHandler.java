package org.skhuconnect.user.exception;

import org.skhuconnect.user.controller.UserActivityController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = UserActivityController.class)
public class UserActivityExceptionHandler {

    @ExceptionHandler(UserActivityException.class)
    ProblemDetail handle(UserActivityException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CURRENT_PASSWORD_MISMATCH -> HttpStatus.UNAUTHORIZED;
            case LOGIN_ID_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(exception.getMessage());
        return detail;
    }

    @ExceptionHandler(UserWithdrawalException.class)
    ProblemDetail handleWithdrawal(UserWithdrawalException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_PASSWORD -> HttpStatus.UNAUTHORIZED;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(exception.getReason() == UserWithdrawalException.Reason.INVALID_PASSWORD
                ? "Current password does not match" : "User not found");
        return detail;
    }
}
