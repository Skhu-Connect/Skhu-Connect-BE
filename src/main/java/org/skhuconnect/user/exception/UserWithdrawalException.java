package org.skhuconnect.user.exception;

public class UserWithdrawalException extends RuntimeException {

    public enum Reason {
        USER_NOT_FOUND,
        INVALID_PASSWORD
    }

    private final Reason reason;

    public UserWithdrawalException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
