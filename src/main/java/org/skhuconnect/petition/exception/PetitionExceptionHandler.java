package org.skhuconnect.petition.exception;

import org.skhuconnect.petition.controller.PetitionController;
import org.skhuconnect.petition.similarity.controller.PetitionSimilarityController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {
        PetitionController.class,
        PetitionSimilarityController.class
})
public class PetitionExceptionHandler {

    @ExceptionHandler(PetitionException.class)
    public ProblemDetail handlePetition(PetitionException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case USER_NOT_FOUND, THRESHOLD_SETTING_NOT_FOUND, PETITION_NOT_FOUND ->
                    HttpStatus.NOT_FOUND;
            case PETITION_FORBIDDEN -> HttpStatus.FORBIDDEN;
            case PETITION_NOT_EDITABLE -> HttpStatus.CONFLICT;
            case PETITION_CREATE_COOLDOWN -> HttpStatus.TOO_MANY_REQUESTS;
            case INVALID_SORT, INVALID_PAGE -> HttpStatus.BAD_REQUEST;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(switch (exception.getReason()) {
            case USER_NOT_FOUND -> "User not found";
            case THRESHOLD_SETTING_NOT_FOUND -> "Threshold setting not found";
            case PETITION_NOT_FOUND -> "Petition not found";
            case PETITION_FORBIDDEN -> "Petition access forbidden";
            case PETITION_NOT_EDITABLE -> "Petition is not editable";
            case PETITION_CREATE_COOLDOWN -> "Petition creation cooldown is active";
            case INVALID_SORT -> "Invalid petition sort property";
            case INVALID_PAGE -> "Invalid petition page request";
        });
        // 쿨다운 429 에만 실린다 - 화면이 "N분 M초 후에 가능"을 계산 없이 그대로 쓴다.
        if (exception.getRetryAfterSeconds() != null) {
            detail.setProperty("retryAfterSeconds", exception.getRetryAfterSeconds());
        }
        return detail;
    }
}
