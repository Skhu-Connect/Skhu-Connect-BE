package org.skhuconnect.notice.exception;

import org.skhuconnect.notice.controller.NoticeController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = NoticeController.class)
public class NoticeExceptionHandler {
    @ExceptionHandler(NoticeException.class)
    public ProblemDetail handle(NoticeException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case USER_NOT_FOUND, NOTICE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_PAGE -> HttpStatus.BAD_REQUEST;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(switch (exception.getReason()) {
            case USER_NOT_FOUND -> "User not found";
            case NOTICE_NOT_FOUND -> "Notice not found";
            case INVALID_PAGE -> "Invalid page request";
        });
        return detail;
    }
}
