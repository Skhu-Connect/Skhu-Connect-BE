package org.skhuconnect.auth.email.service;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;

@Component
public class VerificationCodeGenerator {
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        return format(secureRandom.nextInt(1_000_000));
    }

    static String format(int value) {
        return String.format("%06d", value);
    }
}
