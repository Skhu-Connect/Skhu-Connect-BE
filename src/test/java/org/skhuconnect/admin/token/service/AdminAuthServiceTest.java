package org.skhuconnect.admin.token.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.repository.AdminRepository;
import org.skhuconnect.admin.token.entity.AdminRefreshToken;
import org.skhuconnect.admin.token.exception.AdminAuthException;
import org.skhuconnect.admin.token.repository.AdminRefreshTokenRepository;
import org.skhuconnect.auth.token.dto.TokenIssueResult;
import org.skhuconnect.auth.token.service.AccessTokenService;
import org.skhuconnect.auth.token.service.OpaqueRefreshTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAuthServiceTest {

    private AdminRepository admins;
    private AdminRefreshTokenRepository tokens;
    private PasswordEncoder passwords;
    private AccessTokenService accessTokens;
    private OpaqueRefreshTokenService opaqueTokens;
    private AdminAuthService service;
    private Admin admin;

    @BeforeEach
    void setUp() {
        admins = mock(AdminRepository.class);
        tokens = mock(AdminRefreshTokenRepository.class);
        passwords = mock(PasswordEncoder.class);
        accessTokens = mock(AccessTokenService.class);
        opaqueTokens = mock(OpaqueRefreshTokenService.class);
        service = new AdminAuthService(admins, tokens, passwords, accessTokens,
                opaqueTokens, Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC));
        admin = mock(Admin.class);
        when(accessTokens.issue(admin)).thenReturn("admin-access-token");
    }

    @Test
    void loginStoresOnlyHashAndIssuesAdminTokens() {
        when(admins.findByLoginIdForUpdate("operator")).thenReturn(Optional.of(admin));
        when(passwords.matches("password", admin.getPassword())).thenReturn(true);
        when(opaqueTokens.generate()).thenReturn("raw-refresh");
        when(opaqueTokens.hash("raw-refresh")).thenReturn("a".repeat(64));
        when(tokens.findByAdmin(admin)).thenReturn(Optional.empty());

        TokenIssueResult result = service.login("operator", "password");

        verify(tokens).saveAndFlush(org.mockito.ArgumentMatchers.argThat(token ->
                !token.getTokenHash().equals("raw-refresh")
                        && token.getTokenHash().equals("a".repeat(64))));
        assertThat(result.accessToken()).isEqualTo("admin-access-token");
        assertThat(result.refreshToken()).isEqualTo("raw-refresh");
    }

    @Test
    void invalidCredentialsUseOneGenericReason() {
        when(admins.findByLoginIdForUpdate("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login("missing", "password"))
                .isInstanceOf(AdminAuthException.class)
                .extracting("reason")
                .isEqualTo(AdminAuthException.Reason.INVALID_CREDENTIALS);
    }

    @Test
    void refreshRotatesOnlyAdminRefreshToken() {
        AdminRefreshToken active = AdminRefreshToken.create(admin, "o".repeat(64),
                LocalDateTime.of(2030, 1, 2, 0, 0));
        when(opaqueTokens.hash("old")).thenReturn("o".repeat(64));
        when(tokens.findByTokenHash("o".repeat(64))).thenReturn(Optional.of(active));
        when(opaqueTokens.generate()).thenReturn("new");
        when(opaqueTokens.hash("new")).thenReturn("n".repeat(64));

        TokenIssueResult result = service.refresh("old");

        assertThat(result.refreshToken()).isEqualTo("new");
        assertThat(active.getTokenHash()).isEqualTo("n".repeat(64));
    }
}