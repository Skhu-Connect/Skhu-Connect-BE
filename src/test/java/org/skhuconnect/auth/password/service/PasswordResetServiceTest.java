package org.skhuconnect.auth.password.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.auth.email.entity.EmailVerificationPurpose;
import org.skhuconnect.auth.email.service.EmailVerificationService;
import org.skhuconnect.auth.password.dto.PasswordResetRequest;
import org.skhuconnect.auth.password.exception.PasswordResetException;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {
    private EmailVerificationService emailVerificationService;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        emailVerificationService = mock(EmailVerificationService.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new PasswordResetService(
                emailVerificationService, userRepository, passwordEncoder);
    }

    @Test
    void resetPasswordUsesNormalizedVerifiedEmailAndStoresEncodedPassword() {
        PasswordResetRequest request = new PasswordResetRequest("raw-token", "new-password");
        User user = User.create("student@office.skhu.ac.kr", "student01",
                "old-password", Department.create("CS", "소프트웨어공학과"));
        when(emailVerificationService.consumeToken(
                "raw-token", EmailVerificationPurpose.PASSWORD_RESET))
                .thenReturn("student@office.skhu.ac.kr");
        when(userRepository.findByEmail("student@office.skhu.ac.kr"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-password")).thenReturn("bcrypt-password");

        service.resetPassword(request);

        verify(emailVerificationService).consumeToken(
                "raw-token", EmailVerificationPurpose.PASSWORD_RESET);
        verify(userRepository).findByEmail("student@office.skhu.ac.kr");
        assertThat(user.getPassword()).isEqualTo("bcrypt-password");
        assertThat(user.getPassword()).isNotEqualTo("new-password");
    }

    @Test
    void missingUserIsRejected() {
        when(emailVerificationService.consumeToken(
                "raw-token", EmailVerificationPurpose.PASSWORD_RESET))
                .thenReturn("student@office.skhu.ac.kr");
        when(userRepository.findByEmail("student@office.skhu.ac.kr"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(
                new PasswordResetRequest("raw-token", "new-password")))
                .isInstanceOf(PasswordResetException.class)
                .extracting("reason")
                .isEqualTo(PasswordResetException.Reason.USER_NOT_FOUND);
    }

    @Test
    void resetPasswordHasOneTransactionBoundary() throws Exception {
        assertThat(PasswordResetService.class.getMethod(
                "resetPassword", PasswordResetRequest.class)
                .getAnnotation(Transactional.class)).isNotNull();
    }
}
