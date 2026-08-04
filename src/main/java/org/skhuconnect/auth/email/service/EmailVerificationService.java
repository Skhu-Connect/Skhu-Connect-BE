package org.skhuconnect.auth.email.service;

import org.skhuconnect.auth.email.dto.response.EmailVerificationConfirmResponse;
import org.skhuconnect.auth.email.entity.EmailVerification;
import org.skhuconnect.auth.email.entity.EmailVerificationPurpose;
import org.skhuconnect.auth.email.exception.EmailVerificationException;
import org.skhuconnect.auth.email.exception.EmailVerificationException.Reason;
import org.skhuconnect.auth.email.mail.EmailSender;
import org.skhuconnect.auth.email.repository.EmailVerificationRepository;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class EmailVerificationService {
    private static final long CODE_VALID_MINUTES = 5;
    private static final long TOKEN_VALID_MINUTES = 30;
    private static final long TOKEN_EXPIRES_IN_SECONDS = 1800;

    private final EmailVerificationRepository repository;
    private final UserRepository userRepository;
    private final EmailNormalizer normalizer;
    private final VerificationCodeGenerator codeGenerator;
    private final VerificationTokenGenerator tokenGenerator;
    private final VerificationHasher hasher;
    private final EmailSender emailSender;
    private final Clock clock;

    public EmailVerificationService(
            EmailVerificationRepository repository,
            UserRepository userRepository,
            EmailNormalizer normalizer,
            VerificationCodeGenerator codeGenerator,
            VerificationTokenGenerator tokenGenerator,
            VerificationHasher hasher,
            EmailSender emailSender,
            Clock clock
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.normalizer = normalizer;
        this.codeGenerator = codeGenerator;
        this.tokenGenerator = tokenGenerator;
        this.hasher = hasher;
        this.emailSender = emailSender;
        this.clock = clock;
    }

    @Transactional
    public void sendCode(String email, EmailVerificationPurpose purpose) {
        String normalizedEmail = normalizer.normalize(email);
        requirePurpose(purpose);
        validateAccountState(normalizedEmail, purpose);

        LocalDateTime now = LocalDateTime.now(clock);
        CodeMaterial material = newCode();
        EmailVerification verification = repository.findByEmailAndPurpose(
                        normalizedEmail, purpose)
                .map(existing -> refresh(existing, now, material))
                .orElseGet(() -> create(normalizedEmail, purpose, now, material));

        try {
            repository.saveAndFlush(verification);
        } catch (DataIntegrityViolationException exception) {
            throw error(Reason.RESEND_TOO_SOON);
        }

        emailSender.sendVerificationCode(normalizedEmail, material.rawCode());
    }

    @Transactional(noRollbackFor = EmailVerificationException.class)
    public EmailVerificationConfirmResponse confirm(
            String email,
            String code,
            EmailVerificationPurpose purpose
    ) {
        String normalizedEmail = normalizer.normalize(email);
        requirePurpose(purpose);
        if (code == null || !code.matches("\\d{6}")) {
            throw error(Reason.CODE_MISMATCH);
        }

        EmailVerification verification = repository.findByEmailAndPurpose(
                        normalizedEmail, purpose)
                .orElseThrow(() -> error(Reason.NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(clock);

        validateCodeState(verification, now);
        if (!hasher.matchesCode(
                verification.getCodeHash(), verification.getCodeSalt(), code)) {
            verification.increaseAttemptCount();
            repository.saveAndFlush(verification);
            if (verification.hasReachedAttemptLimit()) {
                throw error(Reason.ATTEMPT_LIMIT_EXCEEDED);
            }
            throw error(Reason.CODE_MISMATCH);
        }

        String token = tokenGenerator.generate();
        verification.verify(
                hasher.hashToken(token), now, now.plusMinutes(TOKEN_VALID_MINUTES));
        return new EmailVerificationConfirmResponse(token, TOKEN_EXPIRES_IN_SECONDS);
    }

    @Transactional
    public String consumeToken(String token, EmailVerificationPurpose purpose) {
        requirePurpose(purpose);
        if (token == null || token.isBlank()) {
            throw error(Reason.TOKEN_INVALID);
        }
        EmailVerification verification = repository.findByTokenHash(
                        hasher.hashToken(token))
                .orElseThrow(() -> error(Reason.TOKEN_INVALID));
        if (verification.getPurpose() != purpose) {
            throw error(Reason.PURPOSE_MISMATCH);
        }
        if (verification.isUsed()) {
            throw error(Reason.TOKEN_USED);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (verification.isTokenExpiredAt(now)) {
            throw error(Reason.TOKEN_EXPIRED);
        }
        verification.consume(now);
        return verification.getEmail();
    }

    private EmailVerification create(
            String email, EmailVerificationPurpose purpose,
            LocalDateTime now, CodeMaterial material) {
        return EmailVerification.create(email, purpose, material.hash(), material.salt(),
                now.plusMinutes(CODE_VALID_MINUTES), now);
    }

    private EmailVerification refresh(EmailVerification verification,
                                      LocalDateTime now, CodeMaterial material) {
        if (!verification.canResendAt(now)) {
            throw error(Reason.RESEND_TOO_SOON);
        }
        verification.refreshCode(material.hash(), material.salt(),
                now.plusMinutes(CODE_VALID_MINUTES), now);
        return verification;
    }

    private CodeMaterial newCode() {
        String code = codeGenerator.generate();
        String salt = hasher.generateSalt();
        return new CodeMaterial(code, salt, hasher.hashCode(salt, code));
    }

    private void validateAccountState(
            String email, EmailVerificationPurpose purpose) {
        boolean exists = userRepository.existsByEmail(email);
        if (purpose == EmailVerificationPurpose.SIGN_UP && exists) {
            throw error(Reason.EMAIL_ALREADY_REGISTERED);
        }
        if (purpose == EmailVerificationPurpose.PASSWORD_RESET && !exists) {
            throw error(Reason.EMAIL_NOT_REGISTERED);
        }
    }

    private void validateCodeState(
            EmailVerification verification, LocalDateTime now) {
        if (verification.isUsed()) {
            throw error(Reason.TOKEN_USED);
        }
        if (verification.isVerified()) {
            throw error(Reason.ALREADY_VERIFIED);
        }
        if (verification.isCodeExpiredAt(now)) {
            throw error(Reason.CODE_EXPIRED);
        }
        if (verification.hasReachedAttemptLimit()) {
            throw error(Reason.ATTEMPT_LIMIT_EXCEEDED);
        }
    }

    private void requirePurpose(EmailVerificationPurpose purpose) {
        if (purpose == null) {
            throw error(Reason.PURPOSE_MISMATCH);
        }
    }

    private EmailVerificationException error(Reason reason) {
        return new EmailVerificationException(reason);
    }

    private record CodeMaterial(String rawCode, String salt, String hash) {
    }
}
