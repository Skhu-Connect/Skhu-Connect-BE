package org.skhuconnect.notice.service;
import org.junit.jupiter.api.Test;
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.notificationlog.entity.*;
import org.skhuconnect.admin.notificationlog.service.AdminNotificationLogService;
import org.skhuconnect.admin.repository.AdminRepository;
import org.skhuconnect.notice.dto.NoticeCreateRequest;
import org.skhuconnect.notice.entity.Notice;
import org.skhuconnect.notice.repository.NoticeRepository;
import org.skhuconnect.notification.service.NotificationEventService;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.util.ReflectionTestUtils;
class NoticeServiceTest {
 @Test void firstPublishNotifiesAndLogsButRepublishDoesNot(){
  NoticeRepository notices=mock(NoticeRepository.class); AdminRepository admins=mock(AdminRepository.class); UserRepository users=mock(UserRepository.class); NotificationEventService events=mock(NotificationEventService.class); AdminNotificationLogService logs=mock(AdminNotificationLogService.class); Admin admin=mock(Admin.class); when(admins.findById(1L)).thenReturn(java.util.Optional.of(admin)); User user=mock(User.class); when(users.findAll()).thenReturn(List.of(user)); NoticeService service=new NoticeService(notices,admins,users,events,logs); Notice notice=Notice.create(admin,"공지 제목","내용"); ReflectionTestUtils.setField(notice,"id",10L); when(notices.findById(10L)).thenReturn(java.util.Optional.of(notice));
  service.publish(1L,10L); service.republish(10L);
  verify(events, times(1)).onNoticePublished(10L,"공지 제목",List.of(user)); verify(logs,times(1)).record(NotificationLogType.NOTICE_PUBLISHED,admin,NotificationLogTargetType.NOTICE,10L,"공지사항 최초 발행"); assertThat(notice.getStatus()).isEqualTo(org.skhuconnect.notice.entity.NoticeStatus.PUBLISHED);
 }
 @Test void publishedQueryDelegatesPublishedStatus(){NoticeRepository notices=mock(NoticeRepository.class); AdminRepository admins=mock(AdminRepository.class); UserRepository users=mock(UserRepository.class); NotificationEventService events=mock(NotificationEventService.class); AdminNotificationLogService logs=mock(AdminNotificationLogService.class); when(notices.findByStatusOrderByCreatedAtDescIdDesc(any(),any())).thenReturn(org.springframework.data.domain.Page.empty()); new NoticeService(notices,admins,users,events,logs).published(0,20); verify(notices).findByStatusOrderByCreatedAtDescIdDesc(org.skhuconnect.notice.entity.NoticeStatus.PUBLISHED,org.springframework.data.domain.PageRequest.of(0,20));}
}
