package org.skhuconnect.auth.token.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private final String secret;
    private final boolean cookieSecure;

    public JwtProperties(String secret, boolean cookieSecure) {
        this.secret = secret;
        this.cookieSecure = cookieSecure;
    }

    public SecretKey secretKey() {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalStateException("JWT_SECRET must be valid Base64", exception);
        }
        if (decoded.length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 256 bits");
        }
        return new SecretKeySpec(decoded, "HmacSHA256");
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }
}