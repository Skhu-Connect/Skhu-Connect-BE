package org.skhuconnect.bookmark.exception;

import org.skhuconnect.bookmark.controller.BookmarkController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = BookmarkController.class)
public class BookmarkExceptionHandler {

    @ExceptionHandler(BookmarkException.class)
    public ProblemDetail handleBookmark(BookmarkException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case USER_NOT_FOUND, PETITION_NOT_FOUND, BOOKMARK_NOT_FOUND ->
                    HttpStatus.NOT_FOUND;
            case BOOKMARK_DUPLICATE -> HttpStatus.CONFLICT;
            case INVALID_PAGE -> HttpStatus.BAD_REQUEST;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(switch (exception.getReason()) {
            case USER_NOT_FOUND -> "User not found";
            case PETITION_NOT_FOUND -> "Petition not found";
            case BOOKMARK_DUPLICATE -> "Petition bookmark already exists";
            case BOOKMARK_NOT_FOUND -> "Petition bookmark not found";
            case INVALID_PAGE -> "Invalid page request";
        });
        return detail;
    }
}
