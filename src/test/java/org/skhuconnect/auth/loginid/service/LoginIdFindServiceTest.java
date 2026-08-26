package org.skhuconnect.auth.loginid.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.auth.email.entity.EmailVerificationPurpose;
import org.skhuconnect.auth.email.service.EmailNormalizer;
import org.skhuconnect.auth.email.service.EmailVerificationService;
import org.skhuconnect.auth.loginid.dto.LoginIdFindEmailRequest;
import org.skhuconnect.auth.loginid.dto.LoginIdFindPasswordRequest;
import org.skhuconnect.auth.token.exception.UserAuthException;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class LoginIdFindServiceTest {

    private EmailVerificationService verifications;
    private UserRepository users;
    private PasswordEncoder passwords;
    private LoginIdFindService service;
    private User user;

    @BeforeEach
    void setUp() {
        verifications = mock(EmailVerificationService.class);
        users = mock(UserRepository.class);
        passwords = mock(PasswordEncoder.class);
        service = new LoginIdFindService(
                verifications, new EmailNormalizer(), users, passwords);
        user = User.create("student@office.skhu.ac.kr", "student01",
                "encoded-password", Department.create("CS", "컴퓨터공학과"));
    }

    @Test
    void findsLoginIdWithOneTimeEmailVerificationToken() {
        when(verifications.consumeToken(
                "raw-token", EmailVerificationPurpose.LOGIN_ID_FIND))
                .thenReturn("student@office.skhu.ac.kr");
        when(users.findByEmail("student@office.skhu.ac.kr"))
                .thenReturn(Optional.of(user));

        var response = service.findByEmailVerification(
                new LoginIdFindEmailRequest("raw-token"));

        assertThat(response.loginId()).isEqualTo("student01");
        verify(verifications).consumeToken(
                "raw-token", EmailVerificationPurpose.LOGIN_ID_FIND);
    }

    @Test
    void findsLoginIdWithNormalizedEmailAndCurrentPassword() {
        when(users.findByEmail("student@office.skhu.ac.kr"))
                .thenReturn(Optional.of(user));
        when(passwords.matches("current-password", "encoded-password"))
                .thenReturn(true);

        var response = service.findByPassword(new LoginIdFindPasswordRequest(
                " STUDENT@OFFICE.SKHU.AC.KR ", "current-password"));

        assertThat(response.loginId()).isEqualTo("student01");
    }

    @Test
    void missingEmailAndWrongPasswordUseSameAuthenticationFailure() {
        when(users.findByEmail("missing@office.skhu.ac.kr"))
                .thenReturn(Optional.empty());
        assertInvalidCredentials(() -> service.findByPassword(
                new LoginIdFindPasswordRequest(
                        "missing@office.skhu.ac.kr", "password")));
        verify(passwords).matches(eq("password"), anyString());

        when(users.findByEmail("student@office.skhu.ac.kr"))
                .thenReturn(Optional.of(user));
        when(passwords.matches("wrong", "encoded-password")).thenReturn(false);
        assertInvalidCredentials(() -> service.findByPassword(
                new LoginIdFindPasswordRequest(
                        "student@office.skhu.ac.kr", "wrong")));
    }

    private void assertInvalidCredentials(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(UserAuthException.class)
                .extracting("reason")
                .isEqualTo(UserAuthException.Reason.INVALID_CREDENTIALS);
    }
}
