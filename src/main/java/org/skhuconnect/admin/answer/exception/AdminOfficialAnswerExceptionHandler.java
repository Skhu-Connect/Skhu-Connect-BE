package org.skhuconnect.admin.answer.exception;

import org.skhuconnect.admin.answer.controller.AdminOfficialAnswerController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdminOfficialAnswerController.class)
public class AdminOfficialAnswerExceptionHandler {

    @ExceptionHandler(AdminOfficialAnswerException.class)
    public ProblemDetail handle(AdminOfficialAnswerException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case ADMIN_NOT_FOUND, PETITION_NOT_FOUND, OFFICIAL_ANSWER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case OFFICIAL_ANSWER_ALREADY_EXISTS, PETITION_NOT_UNDER_REVIEW, PETITION_NOT_ANSWERED -> HttpStatus.CONFLICT;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(switch (exception.getReason()) {
            case ADMIN_NOT_FOUND -> "Administrator not found";
            case PETITION_NOT_FOUND -> "Petition not found";
            case OFFICIAL_ANSWER_NOT_FOUND -> "Official answer not found";
            case OFFICIAL_ANSWER_ALREADY_EXISTS -> "Official answer already exists";
            case PETITION_NOT_UNDER_REVIEW -> "Petition is not under review";
            case PETITION_NOT_ANSWERED -> "Petition is not answered";
        });
        return detail;
    }
}