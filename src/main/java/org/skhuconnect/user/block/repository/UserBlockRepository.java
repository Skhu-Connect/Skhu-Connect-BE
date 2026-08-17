package org.skhuconnect.user.block.repository;

import org.skhuconnect.user.block.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    boolean existsByBlockerIdAndBlockedUserId(Long blockerId, Long blockedUserId);
}
