package org.skhuconnect.user.dto;

import org.skhuconnect.user.entity.User;

public record UserMeResponse(
        String email,
        String loginId,
        String departmentCode,
        String departmentName,
        boolean notificationEnabled
) {
    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getEmail(),
                user.getLoginId(),
                user.getDepartment().getCode(),
                user.getDepartment().getName(),
                user.isNotificationEnabled()
        );
    }
}
