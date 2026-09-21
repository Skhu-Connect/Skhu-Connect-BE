package org.skhuconnect.petition.similarity.exception;

public class PetitionSimilarityException extends RuntimeException {

    public enum Reason {
        USER_NOT_FOUND,
        RATE_LIMIT_EXCEEDED,
        AI_UNAVAILABLE
    }

    private final Reason reason;
    private final Long retryAfterSeconds;

    public PetitionSimilarityException(Reason reason) {
        this(reason, null);
    }

    public PetitionSimilarityException(Reason reason, Long retryAfterSeconds) {
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
