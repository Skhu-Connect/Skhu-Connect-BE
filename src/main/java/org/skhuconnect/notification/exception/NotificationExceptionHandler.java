package org.skhuconnect.notification.exception;

import org.skhuconnect.notification.controller.NotificationController;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = NotificationController.class)
public class NotificationExceptionHandler {
    @ExceptionHandler(NotificationException.class)
    ProblemDetail notFound(NotificationException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        detail.setTitle(exception.getMessage()); return detail;
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setTitle(exception.getMessage()); return detail;
    }
}
