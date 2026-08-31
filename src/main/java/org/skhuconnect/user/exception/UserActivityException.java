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
        INVALID_ACCOUNT_REQUEST("Invalid account update request"),
        CURRENT_PASSWORD_MISMATCH("Current password does not match"),
        LOGIN_ID_UNCHANGED("New login ID must be different"),
        LOGIN_ID_ALREADY_EXISTS("Login ID already exists"),
        PASSWORD_UNCHANGED("New password must be different"),
        INVALID_NOTIFICATION_SETTINGS("At least one notification setting is required"),
        INVALID_PAGE("Invalid user activity page request");

        private final String message;

        Reason(String message) {
            this.message = message;
        }
    }
}
