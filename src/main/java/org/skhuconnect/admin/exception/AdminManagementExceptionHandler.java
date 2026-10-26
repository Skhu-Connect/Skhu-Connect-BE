package org.skhuconnect.admin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AdminManagementExceptionHandler {
    @ExceptionHandler(AdminManagementException.class)
    public ProblemDetail handle(AdminManagementException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
        detail.setTitle("ADMIN_FORBIDDEN");
        return detail;
    }
}
