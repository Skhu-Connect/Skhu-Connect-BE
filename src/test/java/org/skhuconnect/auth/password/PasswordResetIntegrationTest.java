package org.skhuconnect.auth.password;

import org.junit.jupiter.api.Test;
import org.skhuconnect.auth.email.entity.EmailVerification;
import org.skhuconnect.auth.email.entity.EmailVerificationPurpose;
import org.skhuconnect.auth.email.repository.EmailVerificationRepository;
import org.skhuconnect.auth.email.service.VerificationHasher;
import org.skhuconnect.auth.password.dto.PasswordResetRequest;
import org.skhuconnect.auth.password.exception.PasswordResetException;
import org.skhuconnect.auth.password.service.PasswordResetService;
import org.skhuconnect.auth.email.exception.EmailVerificationException;
import org.skhuconnect.auth.token.dto.TokenIssueResult;
import org.skhuconnect.auth.token.exception.UserAuthException;
import org.skhuconnect.auth.token.service.UserAuthService;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "app.mail.from=test@example.com",
        "app.mail.resend-api-key=re_test_key",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.cookie-secure=false"
})
class PasswordResetIntegrationTest {
    @Autowired private PasswordResetService passwordResetService;
    @Autowired private EmailVerificationRepository verificationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private VerificationHasher hasher;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserAuthService userAuthService;

    @Test
    void resetPasswordCommitsBcryptPasswordAndTokenConsumptionTogether() {
        TestData data = createData(true);
        try {
            passwordResetService.resetPassword(
                    new PasswordResetRequest(data.rawToken(), "new-password"));

            User saved = userRepository.findByEmail(data.email()).orElseThrow();
            EmailVerification verification = findVerification(data.rawToken());
            assertThat(passwordEncoder.matches("new-password", saved.getPassword())).isTrue();
            assertThat(saved.getPassword()).isNotEqualTo("new-password");
            assertThat(verification.isUsed()).isTrue();

            assertThatThrownBy(() -> userAuthService.login(
                    saved.getLoginId(), "old-password"))
                    .isInstanceOf(UserAuthException.class)
                    .extracting("reason")
                    .isEqualTo(UserAuthException.Reason.INVALID_CREDENTIALS);

            TokenIssueResult login = userAuthService.login(
                    saved.getLoginId(), "new-password");
            try {
                assertThat(login.accessToken()).isNotBlank();
            } finally {
                userAuthService.logout(login.refreshToken());
            }

            assertThatThrownBy(() -> passwordResetService.resetPassword(
                    new PasswordResetRequest(data.rawToken(), "another-password")))
                    .isInstanceOf(EmailVerificationException.class)
                    .extracting("reason")
                    .isEqualTo(EmailVerificationException.Reason.TOKEN_USED);
        } finally {
            deleteData(data);
        }
    }

    @Test
    void missingUserRollsBackTokenConsumption() {
        TestData data = createData(false);
        try {
            assertThatThrownBy(() -> passwordResetService.resetPassword(
                    new PasswordResetRequest(data.rawToken(), "new-password")))
                    .isInstanceOf(PasswordResetException.class);

            EmailVerification verification = findVerification(data.rawToken());
            assertThat(verification.isUsed()).isFalse();
        } finally {
            deleteData(data);
        }
    }

    private TestData createData(boolean createUser) {
        String unique = UUID.randomUUID().toString().replace("-", "");
        String email = unique + "@office.skhu.ac.kr";
        String rawToken = "token-" + unique;
        Department department = null;
        User user = null;
        if (createUser) {
            department = departmentRepository.saveAndFlush(Department.create(
                    "D" + unique.substring(0, 12), "테스트학과-" + unique.substring(0, 12)));
            user = userRepository.saveAndFlush(User.create(email,
                    "user" + unique.substring(0, 12),
                    passwordEncoder.encode("old-password"), department));
        }
        LocalDateTime now = LocalDateTime.now();
        EmailVerification verification = EmailVerification.create(email,
                EmailVerificationPurpose.PASSWORD_RESET,
                hasher.hashCode("salt", "123456"), "salt",
                now.plusMinutes(5), now);
        verification.verify(hasher.hashToken(rawToken), now, now.plusMinutes(30));
        verificationRepository.saveAndFlush(verification);
        return new TestData(email, rawToken, verification, user, department);
    }

    private void deleteData(TestData data) {
        verificationRepository.delete(data.verification());
        verificationRepository.flush();
        if (data.user() != null) {
            userRepository.delete(data.user());
            userRepository.flush();
            departmentRepository.delete(data.department());
            departmentRepository.flush();
        }
    }

    private EmailVerification findVerification(String rawToken) {
        String tokenHash = hasher.hashToken(rawToken);
        return verificationRepository.findAll().stream()
                .filter(verification -> tokenHash.equals(verification.getTokenHash()))
                .findFirst()
                .orElseThrow();
    }

    private record TestData(String email, String rawToken,
                            EmailVerification verification, User user,
                            Department department) {
    }
}
