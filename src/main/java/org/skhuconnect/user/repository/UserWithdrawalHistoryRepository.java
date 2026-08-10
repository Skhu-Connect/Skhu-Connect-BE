package org.skhuconnect.user.repository;

import org.skhuconnect.user.entity.UserWithdrawalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface UserWithdrawalHistoryRepository
        extends JpaRepository<UserWithdrawalHistory, Long> {

    boolean existsByEmailHashAndWithdrawnAtAfter(
            String emailHash, LocalDateTime withdrawnAfter);
}
