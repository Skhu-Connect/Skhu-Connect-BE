package org.skhuconnect.notification.entity;

import org.junit.jupiter.api.Test;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.user.entity.User;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NotificationTest {
    @Test void createsUnreadAndMarksReadIdempotently() {
        Notification notification = Notification.create(mock(User.class),
                NotificationType.PETITION_UNDER_REVIEW, mock(Petition.class), null, "event:1");
        LocalDateTime now = LocalDateTime.of(2026, 8, 6, 12, 0);
        assertThat(notification.isRead()).isFalse();
        notification.markRead(now);
        notification.markRead(now.plusHours(1));
        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(now);
    }
}
