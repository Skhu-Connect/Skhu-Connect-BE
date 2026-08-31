package org.skhuconnect.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "로그인 아이디 변경 요청")
public record LoginIdUpdateRequest(
        @NotBlank @Size(max = 50)
        @Schema(description = "새 로그인 아이디. 앞뒤 공백은 제거됩니다.", example = "new-login-id", minLength = 1, maxLength = 50)
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
