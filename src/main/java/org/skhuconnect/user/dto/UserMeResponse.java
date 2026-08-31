package org.skhuconnect.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.skhuconnect.user.entity.User;

public record UserMeResponse(
        String email,
        String loginId,
        String departmentCode,
        String departmentName,
        boolean notificationEnabled,
        @Schema(description = "알림 종류별 수신 설정")
        NotificationSettingsResponse notificationSettings
) {
    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getEmail(),
                user.getLoginId(),
                user.getDepartment().getCode(),
                user.getDepartment().getName(),
                user.isNotificationEnabled(),
                NotificationSettingsResponse.from(user)
        );
    }
}
