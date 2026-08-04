package org.skhuconnect.auth.email.repository;

import org.skhuconnect.auth.email.entity.EmailVerification;
import org.skhuconnect.auth.email.entity.EmailVerificationPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

public interface EmailVerificationRepository
        extends JpaRepository<EmailVerification, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerification> findByEmailAndPurpose(
            String email, EmailVerificationPurpose purpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerification> findByTokenHash(String tokenHash);
}
