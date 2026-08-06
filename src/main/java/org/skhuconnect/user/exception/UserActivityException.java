package org.skhuconnect.user.exception;

public class UserActivityException extends RuntimeException {

    private final Reason reason;

    public UserActivityException(Reason reason) {
        super(reason.message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        USER_NOT_FOUND("User not found"),
        INVALID_PAGE("Invalid user activity page request");

        private final String message;

        Reason(String message) {
            this.message = message;
        }
    }
}
