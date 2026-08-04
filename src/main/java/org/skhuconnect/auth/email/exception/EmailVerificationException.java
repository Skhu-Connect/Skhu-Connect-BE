package org.skhuconnect.auth.email.exception;

public class EmailVerificationException extends RuntimeException {
    public enum Reason {
        INVALID_EMAIL, EMAIL_ALREADY_REGISTERED, EMAIL_NOT_REGISTERED,
        RESEND_TOO_SOON, NOT_FOUND, CODE_MISMATCH, CODE_EXPIRED,
        ATTEMPT_LIMIT_EXCEEDED, ALREADY_VERIFIED,
        TOKEN_INVALID, TOKEN_EXPIRED, TOKEN_USED, PURPOSE_MISMATCH
    }

    private final Reason reason;

    public EmailVerificationException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
