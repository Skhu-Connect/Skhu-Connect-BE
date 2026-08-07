package org.skhuconnect.notification.repository;

import java.util.List;
import java.util.Optional;
import org.skhuconnect.notification.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    Optional<FcmToken> findByToken(String token);
    List<FcmToken> findByUserId(Long userId);
    void deleteByUserIdAndToken(Long userId, String token);
}
