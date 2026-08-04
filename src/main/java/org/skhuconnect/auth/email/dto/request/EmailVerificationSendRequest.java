package org.skhuconnect.auth.email.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.skhuconnect.auth.email.entity.EmailVerificationPurpose;

public record EmailVerificationSendRequest(
        @NotBlank @Email String email,
        @NotNull EmailVerificationPurpose purpose
) {
}
