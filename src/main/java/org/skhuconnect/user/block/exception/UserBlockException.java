package org.skhuconnect.user.block.exception;

public class UserBlockException extends RuntimeException {
    public enum Reason { BLOCKER_NOT_FOUND, CONTENT_NOT_FOUND, TARGET_USER_NOT_FOUND, SELF_BLOCK, ALREADY_BLOCKED }
    private final Reason reason;
    public UserBlockException(Reason reason) { super(reason.name()); this.reason = reason; }
    public Reason getReason() { return reason; }
}
