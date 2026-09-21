package org.skhuconnect.petition.similarity.exception;

import org.skhuconnect.petition.similarity.controller.PetitionSimilarityController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PetitionSimilarityController.class)
public class PetitionSimilarityExceptionHandler {

    @ExceptionHandler(PetitionSimilarityException.class)
    public ProblemDetail handle(PetitionSimilarityException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            case AI_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(switch (exception.getReason()) {
            case USER_NOT_FOUND -> "User not found";
            case RATE_LIMIT_EXCEEDED -> "Similar petition search rate limit exceeded";
            case AI_UNAVAILABLE -> "AI similarity search is unavailable";
        });
        if (exception.getRetryAfterSeconds() != null) {
            detail.setProperty("retryAfterSeconds", exception.getRetryAfterSeconds());
        }
        return detail;
    }
}
