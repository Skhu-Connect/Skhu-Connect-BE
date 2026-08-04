package org.skhuconnect.auth.email.dto.response;

public record EmailVerificationConfirmResponse(
        String verificationToken,
        long expiresInSeconds
) {
}
