package org.skhuconnect.auth.loginid.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "가입 이메일과 현재 비밀번호를 이용한 아이디 찾기 요청")
public record LoginIdFindPasswordRequest(
        @NotBlank @Email
        @Schema(description = "가입한 성공회대 이메일", example = "20260000@office.skhu.ac.kr")
        String email,
        @NotBlank
        @Schema(description = "현재 SKHU Connect 비밀번호", example = "current-password")
        String password
) {
}
