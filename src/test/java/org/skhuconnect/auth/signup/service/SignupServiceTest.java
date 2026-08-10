package org.skhuconnect.auth.signup.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skhuconnect.auth.email.entity.EmailVerificationPurpose;
import org.skhuconnect.auth.email.service.EmailVerificationService;
import org.skhuconnect.auth.signup.dto.SignupRequest;
import org.skhuconnect.auth.signup.exception.SignupException;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserWithdrawalHistoryRepository;
import org.skhuconnect.user.service.UserEmailHasher;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignupServiceTest {

    private EmailVerificationService emailVerificationService;
    private UserRepository userRepository;
    private DepartmentRepository departmentRepository;
    private PasswordEncoder passwordEncoder;
    private SignupService service;
    private UserWithdrawalHistoryRepository withdrawalHistories;
    private UserEmailHasher emailHasher;

    @BeforeEach
    void setUp() {
        emailVerificationService = mock(EmailVerificationService.class);
        userRepository = mock(UserRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        withdrawalHistories = mock(UserWithdrawalHistoryRepository.class);
        emailHasher = mock(UserEmailHasher.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new SignupService(emailVerificationService, userRepository,
                departmentRepository, passwordEncoder, withdrawalHistories, emailHasher,
                Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void signupUsesVerifiedEmailAndStoresEncodedPassword() {
        SignupRequest request = request();
        Department department = Department.create("CS", "소프트웨어공학과");
        when(emailVerificationService.consumeToken(
                "raw-token", EmailVerificationPurpose.SIGN_UP))
                .thenReturn("student@office.skhu.ac.kr");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(passwordEncoder.encode("raw-password")).thenReturn("bcrypt-password");

        service.signup(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("student@office.skhu.ac.kr");
        assertThat(saved.getLoginId()).isEqualTo("student01");
        assertThat(saved.getPassword()).isEqualTo("bcrypt-password");
        assertThat(saved.getPassword()).isNotEqualTo("raw-password");
        assertThat(saved.getDepartment()).isSameAs(department);
        assertThat(saved.isNotificationEnabled()).isTrue();
    }

    @Test
    void duplicateLoginIdIsRejected() {
        when(emailVerificationService.consumeToken(
                "raw-token", EmailVerificationPurpose.SIGN_UP))
                .thenReturn("student@office.skhu.ac.kr");
        when(userRepository.existsByLoginId("student01")).thenReturn(true);

        assertThatThrownBy(() -> service.signup(request()))
                .isInstanceOf(SignupException.class)
                .extracting("reason")
                .isEqualTo(SignupException.Reason.LOGIN_ID_ALREADY_EXISTS);
    }

    @Test
    void duplicateEmailIsRejected() {
        when(emailVerificationService.consumeToken(
                "raw-token", EmailVerificationPurpose.SIGN_UP))
                .thenReturn("student@office.skhu.ac.kr");
        when(userRepository.existsByEmail("student@office.skhu.ac.kr"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.signup(request()))
                .isInstanceOf(SignupException.class)
                .extracting("reason")
                .isEqualTo(SignupException.Reason.EMAIL_ALREADY_EXISTS);
    }

    @Test
    void unknownDepartmentIsRejected() {
        when(emailVerificationService.consumeToken(
                "raw-token", EmailVerificationPurpose.SIGN_UP))
                .thenReturn("student@office.skhu.ac.kr");
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.signup(request()))
                .isInstanceOf(SignupException.class)
                .extracting("reason")
                .isEqualTo(SignupException.Reason.DEPARTMENT_NOT_FOUND);
    }

    @Test
    void signupHasOneTransactionBoundary() throws Exception {
        assertThat(SignupService.class.getMethod("signup", SignupRequest.class)
                .getAnnotation(Transactional.class)).isNotNull();
    }

    private SignupRequest request() {
        return new SignupRequest("raw-token", "student01", "raw-password", 1L);
    }
}