package org.skhuconnect.notification.service;

import org.junit.jupiter.api.*;
import org.skhuconnect.notification.entity.*;
import org.skhuconnect.notification.exception.NotificationException;
import org.skhuconnect.notification.repository.NotificationRepository;
import org.springframework.data.domain.PageImpl;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    NotificationRepository repository; NotificationService service;
    @BeforeEach void setUp() {
        repository=mock(NotificationRepository.class);
        service=new NotificationService(repository, Clock.fixed(Instant.parse("2026-08-06T03:00:00Z"), ZoneId.of("Asia/Seoul")));
    }
    @Test void listsNewestAndCountsUnread() {
        when(repository.findByReceiverId(eq(1L), any())).thenReturn(new PageImpl<>(List.of()));
        when(repository.countByReceiverIdAndReadFalse(1L)).thenReturn(3L);
        assertThat(service.findAll(1L,0,20).content()).isEmpty();
        assertThat(service.unreadCount(1L).unreadCount()).isEqualTo(3);
    }
    @Test void marksOneAndAllRead() {
        Notification n=Notification.create(mock(org.skhuconnect.user.entity.User.class),
                NotificationType.COMMENT_LIKE,null,null,"like:1");
        when(repository.findByIdAndReceiverId(5L,1L)).thenReturn(Optional.of(n));
        assertThat(service.markRead(1L,5L).read()).isTrue();
        service.markAllRead(1L);
        verify(repository).markAllRead(eq(1L),any());
        assertThatThrownBy(()->service.markRead(2L,5L)).isInstanceOf(NotificationException.class);
    }
}
