package org.skhuconnect.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 상태 비밀번호 변경 요청")
public record PasswordChangeRequest(
        @NotBlank
        @Schema(description = "현재 비밀번호", example = "current-password")
        String currentPassword,
        @NotBlank
        @Schema(description = "새 비밀번호", example = "new-password")
        String newPassword
) {
}
