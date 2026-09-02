package org.skhuconnect.notice.exception;

public class NoticeException extends RuntimeException {
    public enum Reason {
        USER_NOT_FOUND,
        NOTICE_NOT_FOUND,
        INVALID_PAGE
    }

    private final Reason reason;

    public NoticeException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
