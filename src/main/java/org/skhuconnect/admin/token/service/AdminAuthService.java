package org.skhuconnect.admin.token.service;

import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.repository.AdminRepository;
import org.skhuconnect.admin.token.entity.AdminRefreshToken;
import org.skhuconnect.admin.token.exception.AdminAuthException;
import org.skhuconnect.admin.token.exception.AdminAuthException.Reason;
import org.skhuconnect.admin.token.repository.AdminRefreshTokenRepository;
import org.skhuconnect.auth.token.dto.TokenIssueResult;
import org.skhuconnect.auth.token.service.AccessTokenService;
import org.skhuconnect.auth.token.service.OpaqueRefreshTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AdminAuthService {

    private static final long REFRESH_TOKEN_VALID_DAYS = 14;

    private final AdminRepository adminRepository;
    private final AdminRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final OpaqueRefreshTokenService opaqueTokenService;
    private final Clock clock;

    public AdminAuthService(
            AdminRepository adminRepository,
            AdminRefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AccessTokenService accessTokenService,
            OpaqueRefreshTokenService opaqueTokenService,
            Clock clock
    ) {
        this.adminRepository = adminRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.opaqueTokenService = opaqueTokenService;
        this.clock = clock;
    }

    @Transactional
    public TokenIssueResult login(String loginId, String password) {
        Admin admin = adminRepository.findByLoginIdForUpdate(loginId)
                .orElseThrow(() -> error(Reason.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw error(Reason.INVALID_CREDENTIALS);
        }

        String rawToken = opaqueTokenService.generate();
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusDays(REFRESH_TOKEN_VALID_DAYS);
        AdminRefreshToken refreshToken = refreshTokenRepository.findByAdmin(admin)
                .map(existing -> rotate(existing, rawToken, expiresAt))
                .orElseGet(() -> AdminRefreshToken.create(
                        admin, opaqueTokenService.hash(rawToken), expiresAt));
        refreshTokenRepository.saveAndFlush(refreshToken);
        return result(admin, rawToken);
    }

    @Transactional
    public TokenIssueResult refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw error(Reason.TOKEN_INVALID);
        }
        AdminRefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(opaqueTokenService.hash(rawToken))
                .orElseThrow(() -> error(Reason.TOKEN_INVALID));
        LocalDateTime now = LocalDateTime.now(clock);
        if (refreshToken.isExpiredAt(now)) {
            throw error(Reason.TOKEN_EXPIRED);
        }

        String rotatedRawToken = opaqueTokenService.generate();
        refreshToken.rotate(opaqueTokenService.hash(rotatedRawToken),
                now.plusDays(REFRESH_TOKEN_VALID_DAYS));
        refreshTokenRepository.saveAndFlush(refreshToken);
        return result(refreshToken.getAdmin(), rotatedRawToken);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(opaqueTokenService.hash(rawToken))
                .ifPresent(refreshTokenRepository::delete);
    }

    private AdminRefreshToken rotate(
            AdminRefreshToken token, String rawToken, LocalDateTime expiresAt) {
        token.rotate(opaqueTokenService.hash(rawToken), expiresAt);
        return token;
    }

    private TokenIssueResult result(Admin admin, String rawRefreshToken) {
        return new TokenIssueResult(accessTokenService.issue(admin),
                AccessTokenService.EXPIRES_IN_SECONDS, rawRefreshToken);
    }

    private AdminAuthException error(Reason reason) {
        return new AdminAuthException(reason);
    }
}