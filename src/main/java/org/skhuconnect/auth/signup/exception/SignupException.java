package org.skhuconnect.auth.signup.exception;

public class SignupException extends RuntimeException {

    public enum Reason {
        LOGIN_ID_ALREADY_EXISTS,
        EMAIL_ALREADY_EXISTS,
        DEPARTMENT_NOT_FOUND
    }

    private final Reason reason;

    public SignupException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}