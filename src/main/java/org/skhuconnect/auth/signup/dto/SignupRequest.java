package org.skhuconnect.auth.signup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank String verificationToken,
        @NotBlank @Size(max = 50) String loginId,
        @NotBlank String password,
        @NotNull @Positive Long departmentId
) {
}