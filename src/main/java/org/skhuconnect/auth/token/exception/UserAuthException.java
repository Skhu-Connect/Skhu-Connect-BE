package org.skhuconnect.auth.token.exception;

public class UserAuthException extends RuntimeException {

    public enum Reason {
        INVALID_CREDENTIALS,
        TOKEN_INVALID,
        TOKEN_EXPIRED
    }

    private final Reason reason;

    public UserAuthException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}