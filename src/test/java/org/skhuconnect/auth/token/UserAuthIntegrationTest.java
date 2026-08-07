package org.skhuconnect.auth.token;

import org.junit.jupiter.api.Test;
import org.skhuconnect.auth.token.dto.TokenIssueResult;
import org.skhuconnect.auth.token.repository.RefreshTokenRepository;
import org.skhuconnect.auth.token.service.OpaqueRefreshTokenService;
import org.skhuconnect.auth.token.service.UserAuthService;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "app.mail.from=test@example.com",
        "app.mail.resend-api-key=re_test_key",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.cookie-secure=false"
})
@Transactional
class UserAuthIntegrationTest {

    private static final String TEST_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Autowired UserAuthService service;
    @Autowired UserRepository users;
    @Autowired DepartmentRepository departments;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired OpaqueRefreshTokenService opaqueTokens;

    @Test
    void loginRefreshRotationAndLogoutWorkThroughJpa() {
        String unique = UUID.randomUUID().toString().replace("-", "");
        Department department = departments.saveAndFlush(Department.create(
                "A" + unique.substring(0, 12), "Auth Test"));
        User user = users.saveAndFlush(User.create(
                unique + "@office.skhu.ac.kr",
                "auth" + unique.substring(0, 12),
                passwordEncoder.encode("password"), department));

        TokenIssueResult login = service.login(user.getLoginId(), "password");
        assertThat(refreshTokens.findByTokenHash(
                opaqueTokens.hash(login.refreshToken()))).isPresent();
        assertJwt(login.accessToken(), user.getId());

        TokenIssueResult refreshed = service.refresh(login.refreshToken());
        assertThat(refreshed.refreshToken()).isNotEqualTo(login.refreshToken());
        assertThat(refreshTokens.findByTokenHash(
                opaqueTokens.hash(login.refreshToken()))).isEmpty();
        assertThat(refreshTokens.findByTokenHash(
                opaqueTokens.hash(refreshed.refreshToken()))).isPresent();

        assertThatThrownBy(() -> service.refresh(login.refreshToken()))
                .extracting("reason")
                .isEqualTo(org.skhuconnect.auth.token.exception.UserAuthException.Reason.TOKEN_INVALID);

        service.logout(refreshed.refreshToken());
        assertThat(refreshTokens.findByTokenHash(
                opaqueTokens.hash(refreshed.refreshToken()))).isEmpty();
        service.logout(refreshed.refreshToken());
    }

    private void assertJwt(String rawJwt, Long userId) {
        byte[] key = Base64.getDecoder().decode(TEST_SECRET);
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(key, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        Jwt jwt = decoder.decode(rawJwt);
        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
        assertThat(jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond())
                .isEqualTo(1800);
    }
}