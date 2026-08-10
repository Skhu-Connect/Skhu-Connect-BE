package org.skhuconnect.notification.service;
import org.junit.jupiter.api.Test;
import org.skhuconnect.agreement.repository.AgreementRepository;
import org.skhuconnect.notification.entity.Notification;
import org.skhuconnect.notification.fcm.FcmPushService;
import org.skhuconnect.notification.repository.NotificationRepository;
import org.skhuconnect.user.entity.User;
import java.util.List;
import static org.mockito.Mockito.*;
class NoticeNotificationEventTest {
 @Test void disabledAndDuplicateUsersAreSkipped(){NotificationRepository repo=mock(NotificationRepository.class); AgreementRepository agreements=mock(AgreementRepository.class); FcmPushService fcm=mock(FcmPushService.class); NotificationEventService service=new NotificationEventService(repo,agreements,fcm); User enabled=mock(User.class), disabled=mock(User.class); when(enabled.getId()).thenReturn(1L); when(disabled.getId()).thenReturn(2L); when(enabled.isNotificationEnabled()).thenReturn(true); when(disabled.isNotificationEnabled()).thenReturn(false); when(repo.existsByEventKey("notice:published:3:1")).thenReturn(false); when(repo.existsByEventKey("notice:published:3:2")).thenReturn(false); service.onNoticePublished(3L,"??뺛걠",List.of(enabled,disabled)); }
}
