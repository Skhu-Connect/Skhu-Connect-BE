package org.skhuconnect.auth.token.exception;

public class UserAuthException extends RuntimeException {

    public enum Reason {
        INVALID_CREDENTIALS,
        TOKEN_INVALID,
        TOKEN_EXPIRED,
        ACCOUNT_BANNED
    }

    private final Reason reason;
    private final String detail;

    public UserAuthException(Reason reason) {
        this(reason, null);
    }

    public UserAuthException(Reason reason, String detail) {
        super(reason.name());
        this.reason = reason;
        this.detail = detail;
    }

    public Reason getReason() {
        return reason;
    }

    public String getDetail() {
        return detail;
    }
}