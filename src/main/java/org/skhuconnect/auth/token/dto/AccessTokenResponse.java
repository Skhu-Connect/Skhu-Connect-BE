package org.skhuconnect.auth.token.dto;

public record AccessTokenResponse(
        String accessToken,
        long expiresInSeconds
) {
}