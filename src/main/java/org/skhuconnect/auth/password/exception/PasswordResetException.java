package org.skhuconnect.auth.password.exception;

public class PasswordResetException extends RuntimeException {
    public enum Reason { USER_NOT_FOUND }

    private final Reason reason;

    public PasswordResetException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
