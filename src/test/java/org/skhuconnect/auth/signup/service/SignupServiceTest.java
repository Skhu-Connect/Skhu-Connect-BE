package org.skhuconnect.auth.signup.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skhuconnect.auth.email.entity.EmailVerificationPurpose;
import org.skhuconnect.auth.email.service.EmailVerificationService;
import org.skhuconnect.auth.signup.dto.SignupRequest;
import org.skhuconnect.auth.signup.entity.UserTermsAgreement;
import org.skhuconnect.auth.signup.exception.SignupException;
import org.skhuconnect.auth.signup.repository.UserTermsAgreementRepository;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.skhuconnect.user.repository.UserWithdrawalHistoryRepository;
import org.skhuconnect.user.service.UserEmailHasher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

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

class SignupServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);

    private EmailVerificationService emailVerificationService;
    private UserRepository userRepository;
    private DepartmentRepository departmentRepository;
    private PasswordEncoder passwordEncoder;
    private UserWithdrawalHistoryRepository withdrawalHistories;
    private UserTermsAgreementRepository termsAgreements;
    private UserEmailHasher emailHasher;
    private SignupService service;

    @BeforeEach
    void setUp() {
        emailVerificationService = mock(EmailVerificationService.class);
        userRepository = mock(UserRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        withdrawalHistories = mock(UserWithdrawalHistoryRepository.class);
        termsAgreements = mock(UserTermsAgreementRepository.class);
        emailHasher = mock(UserEmailHasher.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new SignupService(
                emailVerificationService,
                userRepository,
                departmentRepository,
                passwordEncoder,
                withdrawalHistories,
                termsAgreements,
                emailHasher,
                FIXED_CLOCK
        );
    }

    @Test
    void signupStoresUserAndTermsAgreementInOneTransaction() {
        SignupRequest request = request();
        Department department = Department.create("CS", "소프트웨어공학과");
        when(emailVerificationService.consumeToken(
                "raw-token", EmailVerificationPurpose.SIGN_UP))
                .thenReturn("student@office.skhu.ac.kr");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(passwordEncoder.encode("password1")).thenReturn("bcrypt-password");

        service.signup(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("student@office.skhu.ac.kr");
        assertThat(savedUser.getLoginId()).isEqualTo("student01");
        assertThat(savedUser.getPassword()).isEqualTo("bcrypt-password");
        assertThat(savedUser.getDepartment()).isSameAs(department);

        ArgumentCaptor<UserTermsAgreement> agreementCaptor =
                ArgumentCaptor.forClass(UserTermsAgreement.class);
        verify(termsAgreements).save(agreementCaptor.capture());
        UserTermsAgreement agreement = agreementCaptor.getValue();
        assertThat(agreement.getUser()).isSameAs(savedUser);
        assertThat(agreement.getTermsVersion()).isEqualTo("1.0");
        assertThat(agreement.getAgreedAt())
                .isEqualTo(LocalDateTime.of(2030, 1, 1, 0, 0));
    }

    @Test
    void termsNotAgreedIsRejectedBeforeVerificationTokenIsConsumed() {
        SignupRequest request = new SignupRequest(
                "raw-token", "student01", "password1", 1L, false, "1.0");

        assertThatThrownBy(() -> service.signup(request))
                .isInstanceOf(SignupException.class)
                .extracting("reason")
                .isEqualTo(SignupException.Reason.TERMS_NOT_AGREED);
        verify(emailVerificationService, never())
                .consumeToken(any(), any());
        verify(termsAgreements, never()).save(any());
    }

    @Test
    void nullTermsAgreementIsRejectedByService() {
        SignupRequest request = new SignupRequest(
                "raw-token", "student01", "password1", 1L, null, "1.0");

        assertThatThrownBy(() -> service.signup(request))
                .isInstanceOf(SignupException.class)
                .extracting("reason")
                .isEqualTo(SignupException.Reason.TERMS_NOT_AGREED);
    }

    @Test
    void unsupportedTermsVersionIsRejectedBeforeVerificationTokenIsConsumed() {
        SignupRequest request = new SignupRequest(
                "raw-token", "student01", "password1", 1L, true, "2.0");

        assertThatThrownBy(() -> service.signup(request))
                .isInstanceOf(SignupException.class)
                .extracting("reason")
                .isEqualTo(SignupException.Reason.UNSUPPORTED_TERMS_VERSION);
        verify(emailVerificationService, never())
                .consumeToken(any(), any());
        verify(termsAgreements, never()).save(any());
    }

    @Test
    void invalidAccountPolicyIsRejectedBeforeVerificationTokenIsConsumed() {
        SignupRequest request = new SignupRequest(
                "raw-token", "학생12345", "password1", 1L, true, "1.0");

        assertThatThrownBy(() -> service.signup(request))
                .isInstanceOf(SignupException.class)
                .extracting("reason")
                .isEqualTo(SignupException.Reason.INVALID_ACCOUNT_REQUEST);
        verify(emailVerificationService, never())
                .consumeToken(any(), any());
        verify(termsAgreements, never()).save(any());
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
    void userSaveFailureDoesNotSaveTermsAgreement() {
        when(emailVerificationService.consumeToken(
                "raw-token", EmailVerificationPurpose.SIGN_UP))
                .thenReturn("student@office.skhu.ac.kr");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(
                Department.create("CS", "소프트웨어공학과")));
        when(passwordEncoder.encode("password1")).thenReturn("encoded");
        when(userRepository.saveAndFlush(any())).thenThrow(
                new DataIntegrityViolationException("save failed"));

        assertThatThrownBy(() -> service.signup(request()))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(termsAgreements, never()).save(any());
    }

    @Test
    void signupHasOneTransactionBoundary() throws Exception {
        assertThat(SignupService.class.getMethod("signup", SignupRequest.class)
                .getAnnotation(Transactional.class)).isNotNull();
    }

    private SignupRequest request() {
        return new SignupRequest(
                "raw-token", "student01", "password1", 1L, true, "1.0");
    }
}
