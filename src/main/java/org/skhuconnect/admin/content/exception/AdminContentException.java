package org.skhuconnect.admin.content.exception;

public class AdminContentException extends RuntimeException {

    public enum Reason {
        ADMIN_NOT_FOUND,
        PETITION_NOT_FOUND,
        COMMENT_NOT_FOUND,
        INVALID_PAGE
    }

    private final Reason reason;

    public AdminContentException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}