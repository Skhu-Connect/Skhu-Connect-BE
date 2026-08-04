package org.skhuconnect.auth.password.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(
        @NotBlank String verificationToken,
        @NotBlank String newPassword
) {
}
