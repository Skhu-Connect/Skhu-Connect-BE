package org.skhuconnect.user;

import org.junit.jupiter.api.Test;
import org.skhuconnect.auth.token.dto.TokenIssueResult;
import org.skhuconnect.auth.token.exception.UserAuthException;
import org.skhuconnect.auth.token.repository.RefreshTokenRepository;
import org.skhuconnect.auth.token.service.OpaqueRefreshTokenService;
import org.skhuconnect.auth.token.service.UserAuthService;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.notification.entity.FcmToken;
import org.skhuconnect.notification.repository.FcmTokenRepository;
import org.skhuconnect.user.dto.LoginIdUpdateRequest;
import org.skhuconnect.user.dto.PasswordChangeRequest;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.skhuconnect.user.service.UserAccountService;
import org.skhuconnect.user.service.UserActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "app.mail.from=test@example.com",
        "app.mail.resend-api-key=re_test_key",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.cookie-secure=false"
})
class UserAccountIntegrationTest {

    @Autowired private UserAccountService accountService;
    @Autowired private UserActivityService activityService;
    @Autowired private UserAuthService authService;
    @Autowired private UserRepository users;
    @Autowired private DepartmentRepository departments;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private FcmTokenRepository fcmTokens;
    @Autowired private PasswordEncoder passwords;
    @Autowired private OpaqueRefreshTokenService opaqueTokens;

    @Test
    void loginIdChangeUpdatesProfileAndLoginWithoutDeletingSessions() {
        TestData data = createData();
        TokenIssueResult session = authService.login(data.user().getLoginId(), "current-password");
        String newLoginId = "new" + data.unique().substring(0, 12);
        try {
            accountService.changeLoginId(data.user().getId(),
                    new LoginIdUpdateRequest(newLoginId, "current-password"));

            assertThat(activityService.findMe(data.user().getId()).loginId())
                    .isEqualTo(newLoginId);
            assertSessionAndFcmRemain(data, session);
            assertInvalidLogin(data.oldLoginId(), "current-password");

            TokenIssueResult changedLogin = authService.login(
                    newLoginId, "current-password");
            assertThat(changedLogin.accessToken()).isNotBlank();
            authService.logout(changedLogin.refreshToken());
        } finally {
            deleteData(data);
        }
    }

    @Test
    void passwordChangeReplacesLoginPasswordWithoutDeletingSessions() {
        TestData data = createData();
        TokenIssueResult session = authService.login(data.user().getLoginId(), "current-password");
        try {
            accountService.changePassword(data.user().getId(),
                    new PasswordChangeRequest("current-password", "new-password"));

            assertSessionAndFcmRemain(data, session);
            assertInvalidLogin(data.oldLoginId(), "current-password");

            TokenIssueResult changedLogin = authService.login(
                    data.oldLoginId(), "new-password");
            assertThat(changedLogin.accessToken()).isNotBlank();
            authService.logout(changedLogin.refreshToken());
        } finally {
            deleteData(data);
        }
    }

    private TestData createData() {
        String unique = UUID.randomUUID().toString().replace("-", "");
        Department department = departments.saveAndFlush(Department.create(
                "U" + unique.substring(0, 12), "계정변경-" + unique.substring(0, 12)));
        String loginId = "user" + unique.substring(0, 12);
        User user = users.saveAndFlush(User.create(
                unique + "@office.skhu.ac.kr", loginId,
                passwords.encode("current-password"), department));
        FcmToken fcmToken = fcmTokens.saveAndFlush(FcmToken.create(
                user, "fcm-" + unique));
        return new TestData(unique, loginId, user, department, fcmToken);
    }

    private void assertSessionAndFcmRemain(TestData data, TokenIssueResult session) {
        String tokenHash = opaqueTokens.hash(session.refreshToken());
        assertThat(refreshTokens.findAll())
                .anyMatch(token -> token.getTokenHash().equals(tokenHash));
        assertThat(fcmTokens.findByUserId(data.user().getId()))
                .extracting(FcmToken::getToken)
                .containsExactly(data.fcmToken().getToken());
    }

    private void assertInvalidLogin(String loginId, String password) {
        assertThatThrownBy(() -> authService.login(loginId, password))
                .isInstanceOf(UserAuthException.class)
                .extracting("reason")
                .isEqualTo(UserAuthException.Reason.INVALID_CREDENTIALS);
    }

    private void deleteData(TestData data) {
        refreshTokens.deleteAll(refreshTokens.findAll().stream()
                .filter(token -> token.getUser().getId().equals(data.user().getId()))
                .toList());
        fcmTokens.delete(data.fcmToken());
        users.delete(data.user());
        departments.delete(data.department());
    }

    private record TestData(
            String unique,
            String oldLoginId,
            User user,
            Department department,
            FcmToken fcmToken
    ) {
    }
}
