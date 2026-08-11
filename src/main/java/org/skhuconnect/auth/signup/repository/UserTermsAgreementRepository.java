package org.skhuconnect.auth.signup.repository;

import org.skhuconnect.auth.signup.entity.UserTermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTermsAgreementRepository
        extends JpaRepository<UserTermsAgreement, Long> {

    Optional<UserTermsAgreement> findByUserIdAndTermsVersion(
            Long userId, String termsVersion);
}
