package org.skhuconnect.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.skhuconnect.user.entity.User;

@Schema(description = "알림 종류별 수신 설정")
public record NotificationSettingsResponse(
        @Schema(description = "공감 도달 알림 수신 여부", example = "true")
        boolean agreement,
        @Schema(description = "답변 등록 알림 수신 여부", example = "true")
        boolean answer,
        @Schema(description = "답글 알림 수신 여부", example = "true")
        boolean reply,
        @Schema(description = "댓글·답글 공감 알림 수신 여부", example = "false")
        boolean like,
        @Schema(description = "공지사항 알림 수신 여부", example = "true")
        boolean notice
) {
    public static NotificationSettingsResponse from(User user) {
        return new NotificationSettingsResponse(
                user.isNotifyAgreement(),
                user.isNotifyAnswer(),
                user.isNotifyReply(),
                user.isNotifyLike(),
                user.isNotifyNotice()
        );
    }
}
