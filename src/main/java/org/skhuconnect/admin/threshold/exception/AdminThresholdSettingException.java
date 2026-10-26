package org.skhuconnect.admin.threshold.exception;

public class AdminThresholdSettingException extends RuntimeException {

    public enum Reason {
        THRESHOLD_SETTING_NOT_FOUND,
        THRESHOLD_SETTING_ALREADY_EXISTS,
        ADMIN_NOT_FOUND
    }

    private final Reason reason;

    public AdminThresholdSettingException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}
