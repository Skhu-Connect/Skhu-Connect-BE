package org.skhuconnect.auth.password.service;

import org.skhuconnect.auth.email.entity.EmailVerificationPurpose;
import org.skhuconnect.auth.email.service.EmailVerificationService;
import org.skhuconnect.auth.password.dto.PasswordResetRequest;
import org.skhuconnect.auth.password.exception.PasswordResetException;
import org.skhuconnect.auth.validation.AuthValidationPolicy;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
    private final EmailVerificationService emailVerificationService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(EmailVerificationService emailVerificationService,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder) {
        this.emailVerificationService = emailVerificationService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        if (!AuthValidationPolicy.isValidPassword(request.newPassword())) {
            throw new PasswordResetException(
                    PasswordResetException.Reason.INVALID_PASSWORD);
        }
        String normalizedEmail = emailVerificationService.consumeToken(
                request.verificationToken(), EmailVerificationPurpose.PASSWORD_RESET);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new PasswordResetException(
                        PasswordResetException.Reason.USER_NOT_FOUND));
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }
}
