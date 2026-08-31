package org.skhuconnect.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.skhuconnect.auth.validation.AuthValidationPolicy;

@Schema(description = "로그인 아이디 변경 요청")
public record LoginIdUpdateRequest(
        @NotBlank(message = AuthValidationPolicy.LOGIN_ID_REQUIRED_MESSAGE)
        @Pattern(
                regexp = AuthValidationPolicy.LOGIN_ID_PATTERN,
                message = AuthValidationPolicy.LOGIN_ID_MESSAGE
        )
        @Schema(
                description = "새 로그인 아이디. 앞뒤 공백은 제거되며 5~20자의 영문 대소문자, 숫자, 특수문자(_, -, .)만 허용됩니다.",
                example = "new-login-id",
                minLength = 5,
                maxLength = 20
        )
        String newLoginId,
        @NotBlank
        @Schema(description = "현재 비밀번호", example = "current-password")
        String password
) {
    public LoginIdUpdateRequest {
        if (newLoginId != null) {
            newLoginId = newLoginId.trim();
        }
    }
}
