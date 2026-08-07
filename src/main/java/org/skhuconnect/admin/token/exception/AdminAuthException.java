package org.skhuconnect.admin.token.exception;

public class AdminAuthException extends RuntimeException {

    public enum Reason {
        INVALID_CREDENTIALS,
        TOKEN_INVALID,
        TOKEN_EXPIRED
    }

    private final Reason reason;

    public AdminAuthException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}