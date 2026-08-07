package org.skhuconnect.admin.token.repository;

import jakarta.persistence.LockModeType;
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.token.entity.AdminRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AdminRefreshToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AdminRefreshToken> findByAdmin(Admin admin);
}