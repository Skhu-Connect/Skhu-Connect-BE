package org.skhuconnect.auth.signup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank String verificationToken,
        @NotBlank @Size(max = 50) String loginId,
        @NotBlank String password,
        @NotNull @Positive Long departmentId,
        @Schema(description = "필수 이용약관 동의 여부. 반드시 true여야 합니다.", example = "true")
        @NotNull @AssertTrue Boolean termsAgreed,
        @Schema(description = "사용자가 동의한 이용약관 버전. 현재 지원 버전은 1.0입니다.", example = "1.0")
        @NotBlank @Size(max = 20) String termsVersion
) {
}
