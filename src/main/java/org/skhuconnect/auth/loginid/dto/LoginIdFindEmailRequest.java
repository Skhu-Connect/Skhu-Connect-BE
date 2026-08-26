package org.skhuconnect.auth.loginid.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "이메일 인증을 이용한 아이디 찾기 요청")
public record LoginIdFindEmailRequest(
        @NotBlank
        @Schema(description = "LOGIN_ID_FIND 목적으로 발급된 일회용 인증 토큰", example = "verification-token")
        String verificationToken
) {
}
