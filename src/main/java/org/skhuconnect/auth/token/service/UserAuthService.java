package org.skhuconnect.auth.token.service;

import org.skhuconnect.auth.token.dto.TokenIssueResult;
import org.skhuconnect.auth.token.entity.RefreshToken;
import org.skhuconnect.auth.token.exception.UserAuthException;
import org.skhuconnect.auth.token.exception.UserAuthException.Reason;
import org.skhuconnect.auth.token.repository.RefreshTokenRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class UserAuthService {

    private static final long REFRESH_TOKEN_VALID_DAYS = 14;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final OpaqueRefreshTokenService opaqueTokenService;
    private final Clock clock;

    public UserAuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AccessTokenService accessTokenService,
            OpaqueRefreshTokenService opaqueTokenService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.opaqueTokenService = opaqueTokenService;
        this.clock = clock;
    }

    @Transactional
    public TokenIssueResult login(String loginId, String password) {
        User user = userRepository.findByLoginIdForUpdate(loginId)
                .orElseThrow(() -> error(Reason.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw error(Reason.INVALID_CREDENTIALS);
        }

        String rawRefreshToken = opaqueTokenService.generate();
        String tokenHash = opaqueTokenService.hash(rawRefreshToken);
        LocalDateTime expiresAt = LocalDateTime.now(clock)
                .plusDays(REFRESH_TOKEN_VALID_DAYS);
        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .map(existing -> rotate(existing, tokenHash, expiresAt))
                .orElseGet(() -> RefreshToken.create(user, tokenHash, expiresAt));
        refreshTokenRepository.saveAndFlush(refreshToken);
        return result(user, rawRefreshToken);
    }

    @Transactional
    public TokenIssueResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw error(Reason.TOKEN_INVALID);
        }
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(opaqueTokenService.hash(rawRefreshToken))
                .orElseThrow(() -> error(Reason.TOKEN_INVALID));
        LocalDateTime now = LocalDateTime.now(clock);
        if (refreshToken.isExpiredAt(now)) {
            throw error(Reason.TOKEN_EXPIRED);
        }

        String rotatedRawToken = opaqueTokenService.generate();
        refreshToken.rotate(
                opaqueTokenService.hash(rotatedRawToken),
                now.plusDays(REFRESH_TOKEN_VALID_DAYS));
        refreshTokenRepository.saveAndFlush(refreshToken);
        return result(refreshToken.getUser(), rotatedRawToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(
                        opaqueTokenService.hash(rawRefreshToken))
                .ifPresent(refreshTokenRepository::delete);
    }

    private RefreshToken rotate(
            RefreshToken token, String tokenHash, LocalDateTime expiresAt) {
        token.rotate(tokenHash, expiresAt);
        return token;
    }

    private TokenIssueResult result(User user, String rawRefreshToken) {
        return new TokenIssueResult(
                accessTokenService.issue(user),
                AccessTokenService.EXPIRES_IN_SECONDS,
                rawRefreshToken
        );
    }

    private UserAuthException error(Reason reason) {
        return new UserAuthException(reason);
    }
}