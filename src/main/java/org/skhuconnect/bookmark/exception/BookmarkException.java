package org.skhuconnect.bookmark.exception;

public class BookmarkException extends RuntimeException {

    public enum Reason {
        USER_NOT_FOUND,
        PETITION_NOT_FOUND,
        BOOKMARK_DUPLICATE,
        BOOKMARK_NOT_FOUND,
        INVALID_PAGE
    }

    private final Reason reason;

    public BookmarkException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
