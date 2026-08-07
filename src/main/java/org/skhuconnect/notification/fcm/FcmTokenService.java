package org.skhuconnect.notification.fcm;

import org.skhuconnect.notification.dto.request.FcmTokenRequest;
import org.skhuconnect.notification.entity.FcmToken;
import org.skhuconnect.notification.repository.FcmTokenRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FcmTokenService {
    private final FcmTokenRepository tokens; private final UserRepository users;
    public FcmTokenService(FcmTokenRepository tokens, UserRepository users) { this.tokens=tokens; this.users=users; }
    @Transactional public void register(Long userId, FcmTokenRequest request) { User user=users.getReferenceById(userId); tokens.findByToken(request.token()).ifPresentOrElse(existing -> existing.changeUser(user), () -> tokens.save(FcmToken.create(user, request.token()))); }
    @Transactional public void delete(Long userId, String token) { tokens.deleteByUserIdAndToken(userId, token); }
}
