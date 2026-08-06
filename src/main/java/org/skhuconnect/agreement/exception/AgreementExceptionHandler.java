package org.skhuconnect.agreement.exception;

import org.skhuconnect.agreement.controller.AgreementController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AgreementController.class)
public class AgreementExceptionHandler {

    @ExceptionHandler(AgreementException.class)
    public ProblemDetail handleAgreement(AgreementException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case USER_NOT_FOUND, PETITION_NOT_FOUND, AGREEMENT_NOT_FOUND ->
                    HttpStatus.NOT_FOUND;
            case PETITION_NOT_AGREEABLE, AGREEMENT_DUPLICATE ->
                    HttpStatus.CONFLICT;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(switch (exception.getReason()) {
            case USER_NOT_FOUND -> "User not found";
            case PETITION_NOT_FOUND -> "Petition not found";
            case PETITION_NOT_AGREEABLE -> "Petition is not agreeable";
            case AGREEMENT_DUPLICATE -> "Petition agreement already exists";
            case AGREEMENT_NOT_FOUND -> "Petition agreement not found";
        });
        return detail;
    }
}