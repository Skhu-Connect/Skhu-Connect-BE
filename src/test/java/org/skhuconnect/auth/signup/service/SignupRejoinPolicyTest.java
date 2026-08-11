package org.skhuconnect.auth.signup.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skhuconnect.auth.email.entity.EmailVerificationPurpose;
import org.skhuconnect.auth.email.service.EmailVerificationService;
import org.skhuconnect.auth.signup.dto.SignupRequest;
import org.skhuconnect.auth.signup.exception.SignupException;
import org.skhuconnect.auth.signup.repository.UserTermsAgreementRepository;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.skhuconnect.user.repository.UserWithdrawalHistoryRepository;
import org.skhuconnect.user.service.UserEmailHasher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignupRejoinPolicyTest {

    private EmailVerificationService verifications;
    private UserRepository users;
    private DepartmentRepository departments;
    private PasswordEncoder passwords;
    private UserWithdrawalHistoryRepository histories;
    private UserEmailHasher emailHasher;
    private UserTermsAgreementRepository termsAgreements;
    private SignupService service;
    private SignupRequest request;

    @BeforeEach
    void setUp() {
        verifications = mock(EmailVerificationService.class);
        users = mock(UserRepository.class);
        departments = mock(DepartmentRepository.class);
        passwords = mock(PasswordEncoder.class);
        histories = mock(UserWithdrawalHistoryRepository.class);
        emailHasher = mock(UserEmailHasher.class);
        termsAgreements = mock(UserTermsAgreementRepository.class);
        service = new SignupService(verifications, users, departments, passwords,
                histories, termsAgreements, emailHasher,
                Clock.fixed(Instant.parse("2030-01-31T00:00:00Z"), ZoneOffset.UTC));
        request = new SignupRequest(
                "token", "new-login", "password", 1L, true, "1.0");
        when(verifications.consumeToken("token", EmailVerificationPurpose.SIGN_UP))
                .thenReturn("student@office.skhu.ac.kr");
        when(emailHasher.hash("student@office.skhu.ac.kr")).thenReturn("h".repeat(64));
    }

    @Test
    void sameEmailWithinThirtyDaysIsRejected() {
        when(histories.existsByEmailHashAndWithdrawnAtAfter(
                "h".repeat(64), LocalDateTime.of(2030, 1, 1, 0, 0)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.signup(request))
                .isInstanceOf(SignupException.class)
                .extracting("reason")
                .isEqualTo(SignupException.Reason.REJOIN_RESTRICTED);
        verify(users, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void sameEmailAtThirtyDayBoundaryCreatesNewUser() {
        when(departments.findById(1L)).thenReturn(Optional.of(
                Department.create("CS", "소프트웨어공학과")));
        when(passwords.encode("password")).thenReturn("encoded");

        service.signup(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getEmail()).isEqualTo("student@office.skhu.ac.kr");
        verify(histories).existsByEmailHashAndWithdrawnAtAfter(
                "h".repeat(64), LocalDateTime.of(2030, 1, 1, 0, 0));
    }
}
