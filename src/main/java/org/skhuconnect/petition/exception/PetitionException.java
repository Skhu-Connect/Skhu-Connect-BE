package org.skhuconnect.petition.exception;

public class PetitionException extends RuntimeException {

    public enum Reason {
        USER_NOT_FOUND,
        THRESHOLD_SETTING_NOT_FOUND,
        PETITION_NOT_FOUND,
        PETITION_FORBIDDEN,
        PETITION_NOT_EDITABLE,
        PETITION_CREATE_COOLDOWN,
        INVALID_SORT,
        INVALID_PAGE
    }

    private final Reason reason;
    private final Long retryAfterSeconds;

    public PetitionException(Reason reason) {
        this(reason, null);
    }

    /** 쿨다운처럼 "언제 다시 되는지"까지 응답에 실어야 하는 예외용. */
    public PetitionException(Reason reason, Long retryAfterSeconds) {
        super(reason.name());
        this.reason = reason;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Reason getReason() {
        return reason;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}