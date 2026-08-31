package org.skhuconnect.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 종류별 수신 설정 부분 갱신 요청. 생략하거나 null인 항목은 기존 값을 유지합니다.")
public record NotificationSettingsUpdateRequest(
        @Schema(description = "공감 도달 알림 수신 여부", example = "true", nullable = true)
        Boolean agreement,
        @Schema(description = "답변 등록 알림 수신 여부", example = "true", nullable = true)
        Boolean answer,
        @Schema(description = "답글 알림 수신 여부", example = "true", nullable = true)
        Boolean reply,
        @Schema(description = "댓글·답글 공감 알림 수신 여부", example = "false", nullable = true)
        Boolean like,
        @Schema(description = "공지사항 알림 수신 여부", example = "true", nullable = true)
        Boolean notice,
        @Schema(description = "신고 처리 결과·조치 알림 수신 여부", example = "true", nullable = true)
        Boolean report
) {
    @Schema(hidden = true)
    public boolean isEmpty() {
        return agreement == null
                && answer == null
                && reply == null
                && like == null
                && notice == null
                && report == null;
    }
}
