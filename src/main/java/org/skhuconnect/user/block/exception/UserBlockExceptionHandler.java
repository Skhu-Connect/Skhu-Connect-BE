package org.skhuconnect.user.block.exception;

import org.skhuconnect.user.block.controller.UserBlockController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = UserBlockController.class)
public class UserBlockExceptionHandler {
    @ExceptionHandler(UserBlockException.class)
    public ProblemDetail handle(UserBlockException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case BLOCKER_NOT_FOUND, CONTENT_NOT_FOUND, TARGET_USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case SELF_BLOCK -> HttpStatus.BAD_REQUEST;
            case ALREADY_BLOCKED -> HttpStatus.CONFLICT;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(switch (exception.getReason()) {
            case BLOCKER_NOT_FOUND -> "User not found";
            case CONTENT_NOT_FOUND -> "Content not found";
            case TARGET_USER_NOT_FOUND -> "Content writer not found";
            case SELF_BLOCK -> "You cannot block yourself";
            case ALREADY_BLOCKED -> "User is already blocked";
        });
        return detail;
    }
}
