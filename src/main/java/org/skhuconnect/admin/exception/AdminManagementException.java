package org.skhuconnect.admin.exception;

public class AdminManagementException extends RuntimeException {
    public AdminManagementException() {
        super("관리 범위 밖의 작업입니다.");
    }
}
