package org.skhuconnect.admin.answer.exception;

public class AdminOfficialAnswerException extends RuntimeException {

    public enum Reason {
        ADMIN_NOT_FOUND,
        PETITION_NOT_FOUND,
        OFFICIAL_ANSWER_NOT_FOUND,
        OFFICIAL_ANSWER_ALREADY_EXISTS,
        PETITION_NOT_UNDER_REVIEW,
        PETITION_NOT_ANSWERED
    }

    private final Reason reason;

    public AdminOfficialAnswerException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}