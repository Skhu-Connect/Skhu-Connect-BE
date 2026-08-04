package org.skhuconnect.auth.email.service;

import org.skhuconnect.auth.email.exception.EmailVerificationException;
import org.springframework.stereotype.Component;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class EmailNormalizer {
    private static final String SUFFIX = "@office.skhu.ac.kr";
    private static final Pattern FORMAT =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public String normalize(String email) {
        if (email == null) {
            throw invalid();
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!FORMAT.matcher(normalized).matches() || !normalized.endsWith(SUFFIX)) {
            throw invalid();
        }
        return normalized;
    }

    private EmailVerificationException invalid() {
        return new EmailVerificationException(
                EmailVerificationException.Reason.INVALID_EMAIL);
    }
}
