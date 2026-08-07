package org.skhuconnect.admin.token.service;

import org.skhuconnect.auth.token.config.JwtProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AdminRefreshTokenCookieService {

    public static final String COOKIE_NAME = "adminRefreshToken";
    public static final String COOKIE_PATH = "/connect/admin/auth";
    private static final Duration MAX_AGE = Duration.ofDays(14);

    private final JwtProperties properties;

    public AdminRefreshTokenCookieService(JwtProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie issue(String token) {
        return base(token).maxAge(MAX_AGE).build();
    }

    public ResponseCookie expire() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .path(COOKIE_PATH)
                .sameSite(properties.getCookieSameSite());
    }
}