package org.skhuconnect.auth.token.dto;

public record TokenIssueResult(
        String accessToken,
        long expiresInSeconds,
        String refreshToken
) {
}