package org.skhuconnect.user.service;

import org.skhuconnect.auth.email.service.EmailNormalizer;
import org.skhuconnect.auth.token.repository.RefreshTokenRepository;
import org.skhuconnect.notification.repository.FcmTokenRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.entity.UserWithdrawalHistory;
import org.skhuconnect.user.exception.UserWithdrawalException;
import org.skhuconnect.user.repository.UserRepository;
import org.skhuconnect.user.repository.UserWithdrawalHistoryRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class UserWithdrawalService {

    private final UserRepository userRepository;
    private final UserWithdrawalHistoryRepository historyRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNormalizer emailNormalizer;
    private final UserEmailHasher emailHasher;
    private final Clock clock;

    public UserWithdrawalService(
            UserRepository userRepository,
            UserWithdrawalHistoryRepository historyRepository,
            RefreshTokenRepository refreshTokenRepository,
            FcmTokenRepository fcmTokenRepository,
            PasswordEncoder passwordEncoder,
            UserEmailHasher emailHasher,
            EmailNormalizer emailNormalizer,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.fcmTokenRepository = fcmTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailHasher = emailHasher;
        this.clock = clock;
        this.emailNormalizer = emailNormalizer;
    }

    @Transactional
    public void withdraw(Long userId, String currentPassword) {
        User user = userRepository.findByIdForUpdate(userId)
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new UserWithdrawalException(
                        UserWithdrawalException.Reason.USER_NOT_FOUND));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new UserWithdrawalException(
                    UserWithdrawalException.Reason.INVALID_PASSWORD);
        }

        LocalDateTime withdrawnAt = LocalDateTime.now(clock);
        String emailHash = emailHasher.hash(emailNormalizer.normalize(user.getEmail()));

        refreshTokenRepository.deleteByUser(user);
        fcmTokenRepository.deleteAllByUserId(userId);
        user.withdraw(withdrawnAt);
        historyRepository.save(UserWithdrawalHistory.create(
                user, emailHash, withdrawnAt));
    }
}
