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
        HttpStatus status =
                exception.getReason() == UserActivityException.Reason.USER_NOT_FOUND
                        ? HttpStatus.NOT_FOUND
                        : HttpStatus.BAD_REQUEST;
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(exception.getMessage());
        return detail;
    }
}
