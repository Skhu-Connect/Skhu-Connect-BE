package org.skhuconnect.admin.content.exception;

import org.skhuconnect.admin.content.controller.AdminContentController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdminContentController.class)
public class AdminContentExceptionHandler {

    @ExceptionHandler(AdminContentException.class)
    public ProblemDetail handle(AdminContentException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case INVALID_PAGE -> HttpStatus.BAD_REQUEST;
            case ADMIN_NOT_FOUND, PETITION_NOT_FOUND, COMMENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(switch (exception.getReason()) {
            case ADMIN_NOT_FOUND -> "Administrator not found";
            case PETITION_NOT_FOUND -> "Petition not found";
            case COMMENT_NOT_FOUND -> "Comment not found";
            case INVALID_PAGE -> "Invalid page request";
        });
        return detail;
    }
}