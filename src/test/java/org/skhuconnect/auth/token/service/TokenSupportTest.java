package org.skhuconnect.auth.token.service;

import org.junit.jupiter.api.Test;
import org.skhuconnect.auth.token.config.JwtProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenSupportTest {

    @Test
    void opaqueTokenIsRandomAndOnlyItsSha256HashIsFixedLength() {
        OpaqueRefreshTokenService service = new OpaqueRefreshTokenService();
        String first = service.generate();
        String second = service.generate();

        assertThat(first).isNotEqualTo(second);
        assertThat(service.hash(first)).hasSize(64).doesNotContain(first);
    }

    @Test
    void jwtSecretMustBeValidBase64AndAtLeast256Bits() {
        assertThatThrownBy(() -> new JwtProperties("not-base64!", false, "Lax").secretKey())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET must be valid Base64");
        assertThatThrownBy(() -> new JwtProperties("c2hvcnQ=", false, "Lax").secretKey())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET must contain at least 256 bits");
    }

    @Test
    void cookieUsesApprovedSecurityAttributes() {
        RefreshTokenCookieService service = new RefreshTokenCookieService(
                new JwtProperties("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=", true, "None"));
        String issued = service.issue("opaque").toString();
        assertThat(issued).contains("refreshToken=opaque", "Path=/connect/auth",
                "Max-Age=1209600", "HttpOnly", "Secure", "SameSite=None");
        assertThat(service.expire().toString())
                .contains("refreshToken=", "Path=/connect/auth", "Max-Age=0",
                        "HttpOnly", "Secure", "SameSite=None");
    }
}