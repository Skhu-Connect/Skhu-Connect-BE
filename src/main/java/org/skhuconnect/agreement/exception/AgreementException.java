package org.skhuconnect.agreement.exception;

public class AgreementException extends RuntimeException {

    public enum Reason {
        USER_NOT_FOUND,
        PETITION_NOT_FOUND,
        PETITION_NOT_AGREEABLE,
        AGREEMENT_DUPLICATE,
        AGREEMENT_NOT_FOUND,
        SELF_AGREEMENT_NOT_ALLOWED
    }

    private final Reason reason;

    public AgreementException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}