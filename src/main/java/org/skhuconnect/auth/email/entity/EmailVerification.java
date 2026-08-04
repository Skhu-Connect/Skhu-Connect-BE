package org.skhuconnect.auth.email.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.skhuconnect.global.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "email_verifications", indexes = {
        @Index(name = "ux_email_verifications_email_purpose",
                columnList = "email,purpose", unique = true),
        @Index(name = "ux_email_verifications_token_hash",
                columnList = "token_hash", unique = true)
})
public class EmailVerification extends BaseEntity {

    private static final int MAX_ATTEMPT_COUNT = 5;
    private static final long RESEND_WAIT_SECONDS = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "purpose", nullable = false, length = 30)
    private EmailVerificationPurpose purpose;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "code_salt", nullable = false, length = 64)
    private String codeSalt;

    @Column(name = "code_expires_at", nullable = false)
    private LocalDateTime codeExpiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    protected EmailVerification() {
    }

    private EmailVerification(String email, EmailVerificationPurpose purpose,
                              String codeHash, String codeSalt,
                              LocalDateTime codeExpiresAt, LocalDateTime sentAt) {
        this.email = Objects.requireNonNull(email);
        this.purpose = Objects.requireNonNull(purpose);
        refreshCode(codeHash, codeSalt, codeExpiresAt, sentAt);
    }

    public static EmailVerification create(String email, EmailVerificationPurpose purpose,
                                           String codeHash, String codeSalt,
                                           LocalDateTime codeExpiresAt, LocalDateTime sentAt) {
        return new EmailVerification(email, purpose, codeHash, codeSalt, codeExpiresAt, sentAt);
    }

    public void refreshCode(String codeHash, String codeSalt,
                            LocalDateTime codeExpiresAt, LocalDateTime sentAt) {
        this.codeHash = Objects.requireNonNull(codeHash);
        this.codeSalt = Objects.requireNonNull(codeSalt);
        this.codeExpiresAt = Objects.requireNonNull(codeExpiresAt);
        this.sentAt = Objects.requireNonNull(sentAt);
        attemptCount = 0;
        verifiedAt = null;
        tokenHash = null;
        tokenExpiresAt = null;
        usedAt = null;
    }

    public boolean canResendAt(LocalDateTime now) {
        return !now.isBefore(sentAt.plusSeconds(RESEND_WAIT_SECONDS));
    }

    public void increaseAttemptCount() {
        if (attemptCount < MAX_ATTEMPT_COUNT) {
            attemptCount++;
        }
    }

    public boolean hasReachedAttemptLimit() {
        return attemptCount >= MAX_ATTEMPT_COUNT;
    }

    public boolean isCodeExpiredAt(LocalDateTime now) {
        return !now.isBefore(codeExpiresAt);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public void verify(String tokenHash, LocalDateTime now, LocalDateTime expiresAt) {
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.verifiedAt = Objects.requireNonNull(now);
        this.tokenExpiresAt = Objects.requireNonNull(expiresAt);
    }

    public boolean isTokenExpiredAt(LocalDateTime now) {
        return tokenExpiresAt == null || !now.isBefore(tokenExpiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void consume(LocalDateTime now) {
        usedAt = Objects.requireNonNull(now);
    }

    public String getEmail() { return email; }
    public EmailVerificationPurpose getPurpose() { return purpose; }
    public String getCodeHash() { return codeHash; }
    public String getCodeSalt() { return codeSalt; }
    public LocalDateTime getCodeExpiresAt() { return codeExpiresAt; }
    public int getAttemptCount() { return attemptCount; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
}
