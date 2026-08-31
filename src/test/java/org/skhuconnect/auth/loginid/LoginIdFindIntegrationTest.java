package org.skhuconnect.auth.loginid;

import org.junit.jupiter.api.Test;
import org.skhuconnect.auth.email.entity.EmailVerification;
import org.skhuconnect.auth.email.entity.EmailVerificationPurpose;
import org.skhuconnect.auth.email.repository.EmailVerificationRepository;
import org.skhuconnect.auth.email.service.VerificationHasher;
import org.skhuconnect.auth.loginid.dto.LoginIdFindEmailRequest;
import org.skhuconnect.auth.loginid.dto.LoginIdFindPasswordRequest;
import org.skhuconnect.auth.loginid.service.LoginIdFindService;
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

@SpringBootTest(properties = {
        "app.mail.from=test@example.com",
        "app.mail.resend-api-key=re_test_key",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.cookie-secure=false"
})
class LoginIdFindIntegrationTest {

    @Autowired private LoginIdFindService service;
    @Autowired private EmailVerificationRepository verifications;
    @Autowired private VerificationHasher hasher;
    @Autowired private UserRepository users;
    @Autowired private DepartmentRepository departments;
    @Autowired private PasswordEncoder passwords;

    @Test
    void emailVerificationFindsLoginIdAndConsumesTokenOnce() {
        String unique = UUID.randomUUID().toString().replace("-", "");
        String email = unique + "@office.skhu.ac.kr";
        String loginId = "id" + unique.substring(0, 12);
        String rawToken = "token-" + unique;
        Department department = departments.saveAndFlush(Department.create(
                "L" + unique.substring(0, 12), "아이디찾기-" + unique.substring(0, 12)));
        User user = users.saveAndFlush(User.create(
                email, loginId, passwords.encode("password"), department));
        LocalDateTime now = LocalDateTime.now();
        EmailVerification verification = EmailVerification.create(
                email, EmailVerificationPurpose.LOGIN_ID_FIND,
                hasher.hashCode("salt", "123456"), "salt",
                now.plusMinutes(5), now);
        verification.verify(hasher.hashToken(rawToken), now, now.plusMinutes(30));
        verifications.saveAndFlush(verification);

        try {
            assertThat(service.findByEmailVerification(
                    new LoginIdFindEmailRequest(rawToken)).loginId())
                    .isEqualTo(loginId);
            assertThat(verifications.findAll()).anyMatch(EmailVerification::isUsed);
        } finally {
            verifications.delete(verification);
            users.delete(user);
            departments.delete(department);
        }
    }

    @Test
    void emailAndCurrentPasswordFindLoginId() {
        String unique = UUID.randomUUID().toString().replace("-", "");
        String email = unique + "@office.skhu.ac.kr";
        String loginId = "id" + unique.substring(0, 12);
        Department department = departments.saveAndFlush(Department.create(
                "P" + unique.substring(0, 12), "비밀번호확인-" + unique.substring(0, 12)));
        User user = users.saveAndFlush(User.create(
                email, loginId, passwords.encode("current-password"), department));

        try {
            assertThat(service.findByPassword(new LoginIdFindPasswordRequest(
                    " " + email.toUpperCase() + " ", "current-password")).loginId())
                    .isEqualTo(loginId);
        } finally {
            users.delete(user);
            departments.delete(department);
        }
    }
}
