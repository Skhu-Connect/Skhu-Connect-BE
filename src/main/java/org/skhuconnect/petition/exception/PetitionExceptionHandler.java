package org.skhuconnect.petition.exception;

import org.skhuconnect.petition.controller.PetitionController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PetitionController.class)
public class PetitionExceptionHandler {

    @ExceptionHandler(PetitionException.class)
    public ProblemDetail handlePetition(PetitionException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case USER_NOT_FOUND, THRESHOLD_SETTING_NOT_FOUND, PETITION_NOT_FOUND ->
                    HttpStatus.NOT_FOUND;
            case PETITION_FORBIDDEN -> HttpStatus.FORBIDDEN;
            case PETITION_NOT_EDITABLE -> HttpStatus.CONFLICT;
            case INVALID_SORT, INVALID_PAGE -> HttpStatus.BAD_REQUEST;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(switch (exception.getReason()) {
            case USER_NOT_FOUND -> "User not found";
            case THRESHOLD_SETTING_NOT_FOUND -> "Threshold setting not found";
            case PETITION_NOT_FOUND -> "Petition not found";
            case PETITION_FORBIDDEN -> "Petition access forbidden";
            case PETITION_NOT_EDITABLE -> "Petition is not editable";
            case INVALID_SORT -> "Invalid petition sort property";
            case INVALID_PAGE -> "Invalid petition page request";
        });
        return detail;
    }
}