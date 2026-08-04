package org.skhuconnect.auth.email.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skhuconnect.auth.email.dto.response.EmailVerificationConfirmResponse;
import org.skhuconnect.auth.email.entity.EmailVerification;
import org.skhuconnect.auth.email.entity.EmailVerificationPurpose;
import org.skhuconnect.auth.email.exception.EmailDeliveryException;
import org.skhuconnect.auth.email.exception.EmailVerificationException;
import org.skhuconnect.auth.email.mail.EmailSender;
import org.skhuconnect.auth.email.repository.EmailVerificationRepository;
import org.skhuconnect.user.repository.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-04T03:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 4, 3, 0);

    @Mock EmailVerificationRepository repository;
    @Mock UserRepository userRepository;
    @Mock EmailSender emailSender;
    @Mock VerificationCodeGenerator codeGenerator;
    @Mock VerificationTokenGenerator tokenGenerator;

    private VerificationHasher hasher;
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        hasher = new VerificationHasher();
        service = new EmailVerificationService(repository, userRepository,
                new EmailNormalizer(), codeGenerator, tokenGenerator, hasher,
                emailSender, CLOCK);
    }

    @Test
    void sendsSignUpCodeToNormalizedUnregisteredEmail() {
        when(userRepository.existsByEmail("student@office.skhu.ac.kr")).thenReturn(false);
        when(repository.findByEmailAndPurpose(
                "student@office.skhu.ac.kr", EmailVerificationPurpose.SIGN_UP))
                .thenReturn(Optional.empty());
        when(codeGenerator.generate()).thenReturn("012345");

        service.sendCode(" STUDENT@OFFICE.SKHU.AC.KR ",
                EmailVerificationPurpose.SIGN_UP);

        verify(emailSender).sendVerificationCode(
                "student@office.skhu.ac.kr", "012345");
        verify(repository).saveAndFlush(any(EmailVerification.class));
    }

    @Test
    void rejectsAccountStateThatDoesNotMatchPurpose() {
        when(userRepository.existsByEmail("student@office.skhu.ac.kr")).thenReturn(true);
        assertThatThrownBy(() -> service.sendCode(
                "student@office.skhu.ac.kr", EmailVerificationPurpose.SIGN_UP))
                .isInstanceOf(EmailVerificationException.class);
        verify(emailSender, never()).sendVerificationCode(any(), any());

        when(userRepository.existsByEmail("new@office.skhu.ac.kr")).thenReturn(false);
        assertThatThrownBy(() -> service.sendCode(
                "new@office.skhu.ac.kr", EmailVerificationPurpose.PASSWORD_RESET))
                .isInstanceOf(EmailVerificationException.class);
    }

    @Test
    void sendsPasswordResetCodeOnlyForRegisteredEmail() {
        when(userRepository.existsByEmail("student@office.skhu.ac.kr")).thenReturn(true);
        when(repository.findByEmailAndPurpose(
                "student@office.skhu.ac.kr", EmailVerificationPurpose.PASSWORD_RESET))
                .thenReturn(Optional.empty());
        when(codeGenerator.generate()).thenReturn("123456");

        service.sendCode("student@office.skhu.ac.kr",
                EmailVerificationPurpose.PASSWORD_RESET);

        verify(emailSender).sendVerificationCode(
                "student@office.skhu.ac.kr", "123456");
    }

    @Test
    void rejectsResendWithinSixtySeconds() {
        EmailVerification existing = verification("123456", NOW.minusSeconds(30));
        when(userRepository.existsByEmail("student@office.skhu.ac.kr")).thenReturn(false);
        when(repository.findByEmailAndPurpose(any(), any()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.sendCode(
                "student@office.skhu.ac.kr", EmailVerificationPurpose.SIGN_UP))
                .isInstanceOf(EmailVerificationException.class);
        verify(emailSender, never()).sendVerificationCode(any(), any());
    }

    @Test
    void propagatesMailFailureInsteadOfReportingSuccess() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(repository.findByEmailAndPurpose(any(), any())).thenReturn(Optional.empty());
        when(codeGenerator.generate()).thenReturn("123456");
        org.mockito.Mockito.doThrow(new EmailDeliveryException(new RuntimeException()))
                .when(emailSender).sendVerificationCode(any(), any());

        assertThatThrownBy(() -> service.sendCode(
                "student@office.skhu.ac.kr", EmailVerificationPurpose.SIGN_UP))
                .isInstanceOf(EmailDeliveryException.class);
    }

    @Test
    void confirmsCodeAndStoresOnlyTokenHash() {
        EmailVerification verification = verification("123456", NOW);
        when(repository.findByEmailAndPurpose(any(), any()))
                .thenReturn(Optional.of(verification));
        when(tokenGenerator.generate()).thenReturn("raw-token");

        EmailVerificationConfirmResponse response = service.confirm(
                " STUDENT@OFFICE.SKHU.AC.KR ", "123456",
                EmailVerificationPurpose.SIGN_UP);

        assertThat(response.verificationToken()).isEqualTo("raw-token");
        assertThat(response.expiresInSeconds()).isEqualTo(1800);
        assertThat(verification.getTokenHash())
                .isEqualTo(hasher.hashToken("raw-token"))
                .doesNotContain("raw-token");
        assertThat(verification.getTokenExpiresAt()).isEqualTo(NOW.plusMinutes(30));
    }

    @Test
    void wrongCodeIncreasesAttemptsAndFifthFailureDisablesCode() {
        EmailVerification verification = verification("123456", NOW);
        when(repository.findByEmailAndPurpose(any(), any()))
                .thenReturn(Optional.of(verification));

        for (int count = 1; count <= 5; count++) {
            assertThatThrownBy(() -> service.confirm(
                    "student@office.skhu.ac.kr", "654321",
                    EmailVerificationPurpose.SIGN_UP))
                    .isInstanceOf(EmailVerificationException.class);
            assertThat(verification.getAttemptCount()).isEqualTo(count);
        }
        assertThat(verification.hasReachedAttemptLimit()).isTrue();
    }

    @Test
    void rejectsExpiredAndAlreadyVerifiedCode() {
        EmailVerification expired = verification("123456", NOW.minusMinutes(6));
        when(repository.findByEmailAndPurpose(any(), any()))
                .thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.confirm(
                "student@office.skhu.ac.kr", "123456",
                EmailVerificationPurpose.SIGN_UP))
                .isInstanceOf(EmailVerificationException.class);

        EmailVerification verified = verification("123456", NOW);
        verified.verify("hash", NOW, NOW.plusMinutes(30));
        when(repository.findByEmailAndPurpose(any(), any()))
                .thenReturn(Optional.of(verified));
        assertThatThrownBy(() -> service.confirm(
                "student@office.skhu.ac.kr", "123456",
                EmailVerificationPurpose.SIGN_UP))
                .isInstanceOf(EmailVerificationException.class);
    }

    @Test
    void consumesTokenOnceForMatchingPurpose() {
        EmailVerification verification = verification("123456", NOW);
        verification.verify(hasher.hashToken("raw-token"), NOW, NOW.plusMinutes(30));
        when(repository.findByTokenHash(hasher.hashToken("raw-token")))
                .thenReturn(Optional.of(verification));

        assertThat(service.consumeToken(
                "raw-token", EmailVerificationPurpose.SIGN_UP))
                .isEqualTo("student@office.skhu.ac.kr");
        assertThat(verification.isUsed()).isTrue();
        assertThatThrownBy(() -> service.consumeToken(
                "raw-token", EmailVerificationPurpose.SIGN_UP))
                .isInstanceOf(EmailVerificationException.class);
    }

    private EmailVerification verification(String code, LocalDateTime sentAt) {
        String salt = "salt";
        return EmailVerification.create(
                "student@office.skhu.ac.kr", EmailVerificationPurpose.SIGN_UP,
                hasher.hashCode(salt, code), salt, sentAt.plusMinutes(5), sentAt);
    }
}
