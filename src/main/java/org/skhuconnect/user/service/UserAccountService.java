package org.skhuconnect.user.service;

import org.skhuconnect.auth.loginid.dto.LoginIdResponse;
import org.skhuconnect.auth.validation.AuthValidationPolicy;
import org.skhuconnect.user.dto.LoginIdUpdateRequest;
import org.skhuconnect.user.dto.PasswordChangeRequest;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.exception.UserActivityException;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LoginIdResponse changeLoginId(Long userId, LoginIdUpdateRequest request) {
        User user = findUserForUpdate(userId);
        requireCurrentPassword(request.password(), user);
        String newLoginId = request.newLoginId();
        if (!AuthValidationPolicy.isValidLoginId(newLoginId)) {
            throw error(UserActivityException.Reason.INVALID_ACCOUNT_REQUEST);
        }
        if (newLoginId.equals(user.getLoginId())) {
            throw error(UserActivityException.Reason.LOGIN_ID_UNCHANGED);
        }
        if (userRepository.existsByLoginId(newLoginId)) {
            throw error(UserActivityException.Reason.LOGIN_ID_ALREADY_EXISTS);
        }
        user.changeLoginId(newLoginId);
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw error(UserActivityException.Reason.LOGIN_ID_ALREADY_EXISTS);
        }
        return new LoginIdResponse(user.getLoginId());
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = findUserForUpdate(userId);
        requireCurrentPassword(request.currentPassword(), user);
        if (!AuthValidationPolicy.isValidPassword(request.newPassword())) {
            throw error(UserActivityException.Reason.INVALID_ACCOUNT_REQUEST);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw error(UserActivityException.Reason.PASSWORD_UNCHANGED);
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    private User findUserForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> error(UserActivityException.Reason.USER_NOT_FOUND));
    }

    private void requireCurrentPassword(String password, User user) {
        if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw error(UserActivityException.Reason.CURRENT_PASSWORD_MISMATCH);
        }
    }

    private UserActivityException error(UserActivityException.Reason reason) {
        return new UserActivityException(reason);
    }
}
