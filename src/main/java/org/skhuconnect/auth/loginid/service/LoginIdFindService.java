package org.skhuconnect.auth.loginid.service;

import org.skhuconnect.auth.email.entity.EmailVerificationPurpose;
import org.skhuconnect.auth.email.service.EmailNormalizer;
import org.skhuconnect.auth.email.service.EmailVerificationService;
import org.skhuconnect.auth.loginid.dto.LoginIdFindEmailRequest;
import org.skhuconnect.auth.loginid.dto.LoginIdFindPasswordRequest;
import org.skhuconnect.auth.loginid.dto.LoginIdResponse;
import org.skhuconnect.auth.token.exception.UserAuthException;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class LoginIdFindService {

    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final EmailVerificationService emailVerificationService;
    private final EmailNormalizer emailNormalizer;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginIdFindService(
            EmailVerificationService emailVerificationService,
            EmailNormalizer emailNormalizer,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.emailVerificationService = emailVerificationService;
        this.emailNormalizer = emailNormalizer;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LoginIdResponse findByEmailVerification(LoginIdFindEmailRequest request) {
        String email = emailVerificationService.consumeToken(
                request.verificationToken(), EmailVerificationPurpose.LOGIN_ID_FIND);
        return response(userRepository.findByEmail(email)
                .filter(user -> !user.isDeleted())
                .orElseThrow(this::invalidCredentials));
    }

    @Transactional(readOnly = true)
    public LoginIdResponse findByPassword(LoginIdFindPasswordRequest request) {
        String email = emailNormalizer.normalize(request.email());
        Optional<User> user = userRepository.findByEmail(email)
                .filter(found -> !found.isDeleted());
        boolean matches = passwordEncoder.matches(
                request.password(), user.map(User::getPassword)
                        .orElse(DUMMY_PASSWORD_HASH));
        if (user.isEmpty() || !matches) {
            throw invalidCredentials();
        }
        return response(user.orElseThrow());
    }

    private LoginIdResponse response(User user) {
        return new LoginIdResponse(user.getLoginId());
    }

    private UserAuthException invalidCredentials() {
        return new UserAuthException(UserAuthException.Reason.INVALID_CREDENTIALS);
    }
}
