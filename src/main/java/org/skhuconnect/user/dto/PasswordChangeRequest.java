package org.skhuconnect.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.skhuconnect.auth.validation.AuthValidationPolicy;

@Schema(description = "로그인 상태 비밀번호 변경 요청")
public record PasswordChangeRequest(
        @NotBlank
        @Schema(description = "현재 비밀번호", example = "current-password")
        String currentPassword,
        @NotBlank(message = AuthValidationPolicy.PASSWORD_REQUIRED_MESSAGE)
        @Pattern(
                regexp = AuthValidationPolicy.PASSWORD_PATTERN,
                message = AuthValidationPolicy.PASSWORD_MESSAGE
        )
        @Schema(
                description = "새 비밀번호. 5~20자이며 영문과 숫자를 각각 1자 이상 포함해야 합니다. 특수문자는 ! @ # $ % ^ & * ? _ 만 허용됩니다.",
                example = "newPassword1",
                minLength = 5,
                maxLength = 20
        )
        String newPassword
) {
}
