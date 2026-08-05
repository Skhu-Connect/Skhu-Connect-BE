package org.skhuconnect.petition.exception;

public class PetitionException extends RuntimeException {

    public enum Reason {
        USER_NOT_FOUND,
        THRESHOLD_SETTING_NOT_FOUND,
        PETITION_NOT_FOUND,
        PETITION_FORBIDDEN,
        PETITION_NOT_EDITABLE
    }

    private final Reason reason;

    public PetitionException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
