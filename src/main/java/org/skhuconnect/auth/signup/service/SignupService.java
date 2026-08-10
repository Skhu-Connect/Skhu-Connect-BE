package org.skhuconnect.auth.signup.service;

import org.skhuconnect.auth.email.entity.EmailVerificationPurpose;
import org.skhuconnect.auth.email.service.EmailVerificationService;
import org.skhuconnect.auth.signup.dto.SignupRequest;
import org.skhuconnect.auth.signup.exception.SignupException;
import org.skhuconnect.auth.signup.exception.SignupException.Reason;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.skhuconnect.user.repository.UserWithdrawalHistoryRepository;
import org.skhuconnect.user.service.UserEmailHasher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class SignupService {


    private static final long REJOIN_RESTRICTION_DAYS = 30;
    private final EmailVerificationService emailVerificationService;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserWithdrawalHistoryRepository withdrawalHistoryRepository;
    private final UserEmailHasher emailHasher;
    private final Clock clock;

    public SignupService(
            EmailVerificationService emailVerificationService,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder,
            UserWithdrawalHistoryRepository withdrawalHistoryRepository,
            UserEmailHasher emailHasher,
            Clock clock
    ) {
        this.emailVerificationService = emailVerificationService;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.withdrawalHistoryRepository = withdrawalHistoryRepository;
        this.emailHasher = emailHasher;
        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void signup(SignupRequest request) {
        String email = emailVerificationService.consumeToken(
                request.verificationToken(), EmailVerificationPurpose.SIGN_UP);

        if (userRepository.existsByLoginId(request.loginId())) {
            throw new SignupException(Reason.LOGIN_ID_ALREADY_EXISTS);
        }

        LocalDateTime rejoinAllowedBoundary = LocalDateTime.now(clock)
                .minusDays(REJOIN_RESTRICTION_DAYS);
        if (withdrawalHistoryRepository.existsByEmailHashAndWithdrawnAtAfter(
                emailHasher.hash(email), rejoinAllowedBoundary)) {
            throw new SignupException(Reason.REJOIN_RESTRICTED);
        }
        if (userRepository.existsByEmail(email)) {
            throw new SignupException(Reason.EMAIL_ALREADY_EXISTS);
        }

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new SignupException(Reason.DEPARTMENT_NOT_FOUND));
        User user = User.create(
                email,
                request.loginId(),
                passwordEncoder.encode(request.password()),
                department
        );

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateException(exception);
        }
    }

    private SignupException duplicateException(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause().getMessage();
        if (message != null && message.contains("ux_users_login_id")) {
            return new SignupException(Reason.LOGIN_ID_ALREADY_EXISTS);
        }
        if (message != null && message.contains("ux_users_email")) {
            return new SignupException(Reason.EMAIL_ALREADY_EXISTS);
        }
        throw exception;
    }
}