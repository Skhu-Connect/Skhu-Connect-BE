package org.skhuconnect.comment.exception;

public class CommentException extends RuntimeException {

    public enum Reason {
        USER_NOT_FOUND,
        PETITION_NOT_FOUND,
        PETITION_NOT_COMMENTABLE,
        COMMENT_NOT_FOUND,
        COMMENT_FORBIDDEN,
        COMMENT_NOT_EDITABLE,
        PARENT_COMMENT_NOT_FOUND,
        REPLY_DEPTH_EXCEEDED,
        PARENT_COMMENT_DELETED,
        PARENT_COMMENT_HIDDEN,
        COMMENT_LIKE_DUPLICATE,
        COMMENT_LIKE_NOT_FOUND,
        COMMENT_NOT_LIKEABLE,
        ANONYMOUS_NUMBER_CONFLICT,
        INVALID_PAGE
    }

    private final Reason reason;

    public CommentException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
