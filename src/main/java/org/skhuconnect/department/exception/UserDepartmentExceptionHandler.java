package org.skhuconnect.department.exception;

import org.skhuconnect.department.controller.UserDepartmentController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = UserDepartmentController.class)
public class UserDepartmentExceptionHandler {
    @ExceptionHandler(UserDepartmentException.class)
    ProblemDetail handle(UserDepartmentException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        detail.setTitle(exception.getMessage());
        return detail;
    }
}
