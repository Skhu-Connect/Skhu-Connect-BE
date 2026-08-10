package org.skhuconnect.auth.token.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.auth.token.dto.TokenIssueResult;
import org.skhuconnect.auth.token.entity.RefreshToken;
import org.skhuconnect.auth.token.exception.UserAuthException;
import org.skhuconnect.auth.token.repository.RefreshTokenRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAuthServiceTest {

    private UserRepository users;
    private RefreshTokenRepository tokens;
    private PasswordEncoder passwords;
    private AccessTokenService accessTokens;
    private OpaqueRefreshTokenService opaqueTokens;
    private UserAuthService service;
    private User user;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        tokens = mock(RefreshTokenRepository.class);
        passwords = mock(PasswordEncoder.class);
        accessTokens = mock(AccessTokenService.class);
        opaqueTokens = mock(OpaqueRefreshTokenService.class);
        Clock clock = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new UserAuthService(users, tokens, passwords, accessTokens,
                opaqueTokens, clock);
        user = mock(User.class);
        when(accessTokens.issue(user)).thenReturn("access-token");
    }

    @Test
    void loginVerifiesBcryptAndStoresOnlyHash() {
        when(users.findByLoginIdForUpdate("student01")).thenReturn(Optional.of(user));
        when(passwords.matches("plain-password", user.getPassword())).thenReturn(true);
        when(opaqueTokens.generate()).thenReturn("raw-refresh");
        when(opaqueTokens.hash("raw-refresh")).thenReturn("a".repeat(64));
        when(tokens.findByUser(user)).thenReturn(Optional.empty());

        TokenIssueResult result = service.login("student01", "plain-password");
        verify(tokens).saveAndFlush(org.mockito.ArgumentMatchers.argThat(token ->
                token.getTokenHash().equals("a".repeat(64))
                        && !token.getTokenHash().equals("raw-refresh")));
        assertThat(result.refreshToken()).isEqualTo("raw-refresh");
        assertThat(result.expiresInSeconds()).isEqualTo(1800);
    }

    @Test
    void loginReplacesExistingTokenAndInvalidCredentialsAreGeneric() {
        RefreshToken existing = RefreshToken.create(user, "x".repeat(64),
                LocalDateTime.of(2030, 1, 2, 0, 0));
        when(users.findByLoginIdForUpdate("student01")).thenReturn(Optional.of(user));
        when(passwords.matches("plain-password", user.getPassword())).thenReturn(true);
        when(opaqueTokens.generate()).thenReturn("raw-refresh");
        when(opaqueTokens.hash("raw-refresh")).thenReturn("b".repeat(64));
        when(tokens.findByUser(user)).thenReturn(Optional.of(existing));
        service.login("student01", "plain-password");
        assertThat(existing.getTokenHash()).isEqualTo("b".repeat(64));
        when(users.findByLoginIdForUpdate("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login("missing", "anything"))
                .isInstanceOf(UserAuthException.class)
                .extracting("reason")
                .isEqualTo(UserAuthException.Reason.INVALID_CREDENTIALS);
    }

    @Test
    void refreshRotatesActiveTokenAndRejectsMissingOrExpiredToken() {
        RefreshToken active = RefreshToken.create(user, "o".repeat(64),
                LocalDateTime.of(2030, 1, 2, 0, 0));
        when(opaqueTokens.hash("old")).thenReturn("o".repeat(64));
        when(tokens.findByTokenHash("o".repeat(64))).thenReturn(Optional.of(active));
        when(opaqueTokens.generate()).thenReturn("new");
        when(opaqueTokens.hash("new")).thenReturn("n".repeat(64));
        TokenIssueResult result = service.refresh("old");
        assertThat(result.refreshToken()).isEqualTo("new");
        assertThat(active.getTokenHash()).isEqualTo("n".repeat(64));
        when(opaqueTokens.hash("missing")).thenReturn("m".repeat(64));
        when(tokens.findByTokenHash("m".repeat(64))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.refresh("missing"))
                .extracting("reason").isEqualTo(UserAuthException.Reason.TOKEN_INVALID);
        RefreshToken expired = RefreshToken.create(user, "e".repeat(64),
                LocalDateTime.of(2030, 1, 1, 0, 0));
        when(opaqueTokens.hash("expired")).thenReturn("e".repeat(64));
        when(tokens.findByTokenHash("e".repeat(64))).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.refresh("expired"))
                .extracting("reason").isEqualTo(UserAuthException.Reason.TOKEN_EXPIRED);
    }

    @Test
    void logoutIsIdempotent() {
        service.logout(null);
        verify(tokens, never()).delete(org.mockito.ArgumentMatchers.any());
        RefreshToken active = RefreshToken.create(user, "a".repeat(64),
                LocalDateTime.of(2030, 1, 2, 0, 0));
        when(opaqueTokens.hash("active")).thenReturn("a".repeat(64));
        when(tokens.findByTokenHash("a".repeat(64))).thenReturn(Optional.of(active));
        service.logout("active");
        verify(tokens).delete(active);
    }

    @Test
    void withdrawnUserCannotLogin() {
        when(users.findByLoginIdForUpdate("withdrawn")).thenReturn(Optional.of(user));
        when(user.isDeleted()).thenReturn(true);

        assertThatThrownBy(() -> service.login("withdrawn", "password"))
                .isInstanceOf(UserAuthException.class)
                .extracting("reason")
                .isEqualTo(UserAuthException.Reason.INVALID_CREDENTIALS);
        verify(passwords, never()).matches(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void withdrawnUserCannotRefresh() {
        RefreshToken token = RefreshToken.create(user, "w".repeat(64),
                LocalDateTime.of(2030, 1, 2, 0, 0));
        when(opaqueTokens.hash("withdrawn-token")).thenReturn("w".repeat(64));
        when(tokens.findByTokenHash("w".repeat(64))).thenReturn(Optional.of(token));
        when(user.isDeleted()).thenReturn(true);

        assertThatThrownBy(() -> service.refresh("withdrawn-token"))
                .isInstanceOf(UserAuthException.class)
                .extracting("reason")
                .isEqualTo(UserAuthException.Reason.TOKEN_INVALID);
    }

    @Test
    void operationsHaveTransactionBoundaries() throws Exception {
        assertThat(UserAuthService.class.getMethod("login", String.class, String.class)
                .getAnnotation(Transactional.class)).isNotNull();
        assertThat(UserAuthService.class.getMethod("refresh", String.class)
                .getAnnotation(Transactional.class)).isNotNull();
        assertThat(UserAuthService.class.getMethod("logout", String.class)
                .getAnnotation(Transactional.class)).isNotNull();
    }
}
