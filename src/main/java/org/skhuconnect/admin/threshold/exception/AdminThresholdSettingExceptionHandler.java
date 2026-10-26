package org.skhuconnect.admin.threshold.exception;

import org.skhuconnect.admin.threshold.controller.AdminThresholdSettingController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdminThresholdSettingController.class)
public class AdminThresholdSettingExceptionHandler {

    @ExceptionHandler(AdminThresholdSettingException.class)
    public ProblemDetail handle(AdminThresholdSettingException exception) {
        HttpStatus status = exception.getReason() == AdminThresholdSettingException.Reason.THRESHOLD_SETTING_ALREADY_EXISTS
                ? HttpStatus.CONFLICT : HttpStatus.NOT_FOUND;
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(switch (exception.getReason()) {
            case THRESHOLD_SETTING_NOT_FOUND -> "Threshold setting not found";
            case THRESHOLD_SETTING_ALREADY_EXISTS -> "Threshold setting already exists";
            case ADMIN_NOT_FOUND -> "Administrator not found";
        });
        return detail;
    }
}
