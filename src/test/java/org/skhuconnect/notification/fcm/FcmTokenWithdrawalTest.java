package org.skhuconnect.notification.fcm;

import org.junit.jupiter.api.Test;
import org.skhuconnect.notification.dto.request.FcmTokenRequest;
import org.skhuconnect.notification.repository.FcmTokenRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FcmTokenWithdrawalTest {

    @Test
    void withdrawnUserCannotRegisterFcmToken() {
        FcmTokenRepository tokens = mock(FcmTokenRepository.class);
        UserRepository users = mock(UserRepository.class);
        User user = mock(User.class);
        when(user.isDeleted()).thenReturn(true);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        FcmTokenService service = new FcmTokenService(tokens, users);

        assertThatThrownBy(() -> service.register(1L, new FcmTokenRequest("token")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(tokens, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
