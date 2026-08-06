package org.skhuconnect.comment.exception;

import org.skhuconnect.comment.controller.CommentController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CommentController.class)
public class CommentExceptionHandler {

    @ExceptionHandler(CommentException.class)
    public ProblemDetail handleComment(CommentException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case USER_NOT_FOUND, PETITION_NOT_FOUND, COMMENT_NOT_FOUND,
                    COMMENT_LIKE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case COMMENT_FORBIDDEN -> HttpStatus.FORBIDDEN;
            case INVALID_PAGE -> HttpStatus.BAD_REQUEST;
            case PETITION_NOT_COMMENTABLE, COMMENT_NOT_EDITABLE,
                    COMMENT_LIKE_DUPLICATE, COMMENT_NOT_LIKEABLE,
                    ANONYMOUS_NUMBER_CONFLICT -> HttpStatus.CONFLICT;
        };
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(switch (exception.getReason()) {
            case USER_NOT_FOUND -> "User not found";
            case PETITION_NOT_FOUND -> "Petition not found";
            case PETITION_NOT_COMMENTABLE -> "Petition is not commentable";
            case COMMENT_NOT_FOUND -> "Comment not found";
            case COMMENT_FORBIDDEN -> "Comment access forbidden";
            case COMMENT_NOT_EDITABLE -> "Comment is not editable";
            case COMMENT_LIKE_DUPLICATE -> "Comment like already exists";
            case COMMENT_LIKE_NOT_FOUND -> "Comment like not found";
            case COMMENT_NOT_LIKEABLE -> "Comment is not likeable";
            case ANONYMOUS_NUMBER_CONFLICT -> "Anonymous number conflict";
            case INVALID_PAGE -> "Invalid comment page request";
        });
        return detail;
    }
}
