package org.skhuconnect.auth.password;

import org.junit.jupiter.api.Test;
import org.skhuconnect.auth.email.entity.EmailVerification;
import org.skhuconnect.auth.email.entity.EmailVerificationPurpose;
import org.skhuconnect.auth.email.repository.EmailVerificationRepository;
import org.skhuconnect.auth.email.service.VerificationHasher;
import org.skhuconnect.auth.password.dto.PasswordResetRequest;
import org.skhuconnect.auth.password.service.PasswordResetService;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "app.mail.from=test@example.com",
        "app.mail.resend-api-key=re_test_key",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.cookie-secure=false"
})
class PasswordResetFailureIntegrationTest {
    @Autowired private PasswordResetService passwordResetService;
    @Autowired private EmailVerificationRepository verificationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private VerificationHasher hasher;
    @MockitoBean private PasswordEncoder passwordEncoder;

    @Test
    void passwordEncodingFailureRollsBackTokenConsumptionAndPasswordChange() {
        String unique = UUID.randomUUID().toString().replace("-", "");
        String email = unique + "@office.skhu.ac.kr";
        String rawToken = "token-" + unique;
        Department department = departmentRepository.saveAndFlush(Department.create(
                "D" + unique.substring(0, 12), "테스트학과-" + unique.substring(0, 12)));
        User user = userRepository.saveAndFlush(User.create(email,
                "user" + unique.substring(0, 12), "old-encoded-password", department));
        LocalDateTime now = LocalDateTime.now();
        EmailVerification verification = EmailVerification.create(email,
                EmailVerificationPurpose.PASSWORD_RESET,
                hasher.hashCode("salt", "123456"), "salt",
                now.plusMinutes(5), now);
        verification.verify(hasher.hashToken(rawToken), now, now.plusMinutes(30));
        verificationRepository.saveAndFlush(verification);
        when(passwordEncoder.encode("new-password"))
                .thenThrow(new IllegalStateException("encoding failed"));

        try {
            assertThatThrownBy(() -> passwordResetService.resetPassword(
                    new PasswordResetRequest(rawToken, "new-password")))
                    .isInstanceOf(IllegalStateException.class);

            User unchanged = userRepository.findByEmail(email).orElseThrow();
            EmailVerification notConsumed = verificationRepository.findAll().stream()
                    .filter(candidate -> hasher.hashToken(rawToken)
                            .equals(candidate.getTokenHash()))
                    .findFirst()
                    .orElseThrow();
            assertThat(unchanged.getPassword()).isEqualTo("old-encoded-password");
            assertThat(notConsumed.isUsed()).isFalse();
        } finally {
            verificationRepository.delete(verification);
            verificationRepository.flush();
            userRepository.delete(user);
            userRepository.flush();
            departmentRepository.delete(department);
            departmentRepository.flush();
        }
    }
}
