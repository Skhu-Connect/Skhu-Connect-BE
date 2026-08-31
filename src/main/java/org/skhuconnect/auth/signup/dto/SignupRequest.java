package org.skhuconnect.auth.signup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.skhuconnect.auth.validation.AuthValidationPolicy;

public record SignupRequest(
        @NotBlank String verificationToken,
        @Schema(
                description = "로그인 아이디. 5~20자의 영문 대소문자, 숫자, 특수문자(_, -, .)만 허용됩니다.",
                example = "student01",
                minLength = 5,
                maxLength = 20
        )
        @NotBlank(message = AuthValidationPolicy.LOGIN_ID_REQUIRED_MESSAGE)
        @Pattern(
                regexp = AuthValidationPolicy.LOGIN_ID_PATTERN,
                message = AuthValidationPolicy.LOGIN_ID_MESSAGE
        )
        String loginId,
        @Schema(
                description = "비밀번호. 5~20자이며 영문과 숫자를 각각 1자 이상 포함해야 합니다. 특수문자는 ! @ # $ % ^ & * ? _ 만 허용됩니다.",
                example = "password1",
                minLength = 5,
                maxLength = 20
        )
        @NotBlank(message = AuthValidationPolicy.PASSWORD_REQUIRED_MESSAGE)
        @Pattern(
                regexp = AuthValidationPolicy.PASSWORD_PATTERN,
                message = AuthValidationPolicy.PASSWORD_MESSAGE
        )
        String password,
        @NotNull @Positive Long departmentId,
        @Schema(description = "필수 이용약관 동의 여부. 반드시 true여야 합니다.", example = "true")
        @NotNull @AssertTrue Boolean termsAgreed,
        @Schema(description = "사용자가 동의한 이용약관 버전. 현재 지원 버전은 1.0입니다.", example = "1.0")
        @NotBlank @Size(max = 20) String termsVersion
) {
}
