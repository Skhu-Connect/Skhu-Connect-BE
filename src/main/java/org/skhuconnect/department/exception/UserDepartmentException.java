package org.skhuconnect.department.exception;

public class UserDepartmentException extends RuntimeException {
    private final Reason reason;

    public UserDepartmentException(Reason reason) {
        super(reason.message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        USER_NOT_FOUND("사용자를 찾을 수 없습니다"),
        DEPARTMENT_NOT_FOUND("학과를 찾을 수 없습니다");

        private final String message;

        Reason(String message) {
            this.message = message;
        }
    }
}
