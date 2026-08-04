package org.skhuconnect.auth.email.service;

import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

@Component
public class VerificationHasher {
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSalt() {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    public String hashCode(String salt, String code) {
        return sha256(salt + ":" + code);
    }

    public String hashToken(String token) {
        return sha256(token);
    }

    public boolean matchesCode(String expected, String salt, String code) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                hashCode(salt, code).getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
