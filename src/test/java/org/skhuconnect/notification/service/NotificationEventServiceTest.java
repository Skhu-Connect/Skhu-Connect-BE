package org.skhuconnect.notification.service;

import org.junit.jupiter.api.*;
import org.skhuconnect.agreement.repository.AgreementRepository;
import org.skhuconnect.agreement.entity.Agreement;
import org.mockito.ArgumentCaptor;
import org.skhuconnect.comment.entity.*;
import org.skhuconnect.notification.dto.NotificationResponse;
import org.skhuconnect.notification.entity.Notification;
import org.skhuconnect.notification.entity.NotificationType;
import org.skhuconnect.notification.entity.NotificationPoint;
import org.skhuconnect.notification.fcm.FcmPushService;
import org.skhuconnect.notification.repository.NotificationRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.user.entity.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationEventServiceTest {
    NotificationRepository notifications; AgreementRepository agreements; ApplicationEventPublisher events; NotificationEventService service;
    @BeforeEach void setUp(){ notifications=mock(NotificationRepository.class); agreements=mock(AgreementRepository.class);
        when(notifications.saveAndFlush(any(Notification.class))).thenAnswer(call -> call.getArgument(0));
        events=mock(ApplicationEventPublisher.class); service=new NotificationEventService(notifications,agreements,events); }
    @Test void thresholdEventsAreCreatedOnceAndWriterIsNotDuplicated() {
        User writer=user(1L); Petition petition=mock(Petition.class);
        when(petition.getId()).thenReturn(10L); when(petition.getWriter()).thenReturn(writer);
        when(petition.getAgreementCount()).thenReturn(10); when(petition.getTargetAgreementCount()).thenReturn(10);
        User supporter=user(2L);
        Agreement supporterAgreement=mock(Agreement.class); when(supporterAgreement.getUser()).thenReturn(supporter);
        Agreement writerAgreement=mock(Agreement.class); when(writerAgreement.getUser()).thenReturn(writer);
        when(agreements.findByPetitionId(10L)).thenReturn(List.of(supporterAgreement, writerAgreement));
        service.onAgreementAdded(petition,5);
        verify(notifications,times(4)).saveAndFlush(any(Notification.class));
    }
    @Test void duplicateEventKeyAndDisabledReceiverPreventCreation() {
        User writer=user(1L); when(writer.isNotificationEnabled()).thenReturn(true);
        Petition p=mock(Petition.class); when(p.getId()).thenReturn(10L); when(p.getWriter()).thenReturn(writer);
        when(p.getAgreementCount()).thenReturn(6); when(p.getTargetAgreementCount()).thenReturn(10);
        when(notifications.existsByEventKey(any())).thenReturn(true);
        service.onAgreementAdded(p,5);
        verify(notifications,never()).saveAndFlush(any());
    }
    @Test void answerNotificationUsesWriterAndAgreementAudienceWithExistingDeduplicationPolicy() {
        User writer=user(1L); User supporter=user(2L); Petition petition=mock(Petition.class);
        when(petition.getId()).thenReturn(10L); when(petition.getWriter()).thenReturn(writer);
        Agreement supporterAgreement=mock(Agreement.class); when(supporterAgreement.getUser()).thenReturn(supporter);
        Agreement writerAgreement=mock(Agreement.class); when(writerAgreement.getUser()).thenReturn(writer);
        when(agreements.findByPetitionId(10L)).thenReturn(List.of(supporterAgreement, writerAgreement));

        service.onPetitionAnswered(petition);

        verify(notifications,times(2)).saveAndFlush(any(Notification.class));
    }
    @Test void replyAndLikeNotifyOwnerButNotSelf() {
        User owner=user(1L), actor=user(2L); Petition petition=mock(Petition.class); when(petition.getId()).thenReturn(10L);
        PetitionAnonymousNumber ownerMap=PetitionAnonymousNumber.create(petition,owner,1);
        PetitionAnonymousNumber actorMap=PetitionAnonymousNumber.create(petition,actor,2);
        Comment root=Comment.create(petition,owner,ownerMap,"root"); ReflectionTestUtils.setField(root,"id",20L);
        Comment reply=Comment.create(petition,actor,actorMap,root,"reply"); ReflectionTestUtils.setField(reply,"id",21L);
        service.onReplyCreated(reply); service.onCommentLiked(root,actor); service.onCommentLiked(reply,owner);
        verify(notifications,times(3)).saveAndFlush(any(Notification.class));
        service.onCommentLiked(root,owner);
        verify(notifications,times(3)).saveAndFlush(any(Notification.class));
    }
    @Test void disabledReceiverDoesNotReceiveNotification() {
        User writer=user(1L);
        when(writer.isNotificationEnabled()).thenReturn(false);
        Petition petition=mock(Petition.class);
        when(petition.getId()).thenReturn(10L);
        when(petition.getWriter()).thenReturn(writer);
        when(petition.getAgreementCount()).thenReturn(5);
        when(petition.getTargetAgreementCount()).thenReturn(10);

        service.onAgreementAdded(petition,5);

        verify(notifications,never()).saveAndFlush(any());
    }
    @Test void disabledPointBlocksOnlyItsNotificationType() {
        User owner=user(1L), actor=user(2L);
        when(owner.allows(NotificationPoint.LIKE)).thenReturn(false);
        Petition petition=mock(Petition.class); when(petition.getId()).thenReturn(10L);
        PetitionAnonymousNumber ownerMap=PetitionAnonymousNumber.create(petition,owner,1);
        PetitionAnonymousNumber actorMap=PetitionAnonymousNumber.create(petition,actor,2);
        Comment root=Comment.create(petition,owner,ownerMap,"root"); ReflectionTestUtils.setField(root,"id",20L);
        Comment reply=Comment.create(petition,actor,actorMap,root,"reply"); ReflectionTestUtils.setField(reply,"id",21L);

        service.onCommentLiked(root,actor);
        service.onReplyCreated(reply);

        verify(notifications,times(1)).saveAndFlush(any(Notification.class));
    }
    @Test void pushMessageIsPublishedWithValuesSnapshottedInsideTransaction() {
        User writer=user(1L); Petition petition=mock(Petition.class);
        when(petition.getId()).thenReturn(10L); when(petition.getWriter()).thenReturn(writer);
        when(agreements.findByPetitionId(10L)).thenReturn(List.of());

        service.onPetitionAnswered(petition);

        ArgumentCaptor<FcmPushService.PushMessage> captor=ArgumentCaptor.forClass(FcmPushService.PushMessage.class);
        verify(events).publishEvent(captor.capture());
        FcmPushService.PushMessage published=captor.getValue();
        assertThat(published.receiverId()).isEqualTo(1L);
        assertThat(published.petitionId()).isEqualTo(10L);
        assertThat(published.title()).isEqualTo("SKHU Connect");
        Notification expected=mock(Notification.class); when(expected.getType()).thenReturn(NotificationType.PETITION_ANSWERED);
        assertThat(published.body()).isEqualTo(NotificationResponse.message(expected));
    }
    @Test void reportProcessedNotifiesReporterAndTarget() {
        User reporter=user(1L); User writer=user(2L);
        Petition petition=mock(Petition.class); when(petition.getWriter()).thenReturn(writer);
        org.skhuconnect.report.entity.Report report=org.skhuconnect.report.entity.Report.forPetition(
                reporter, petition, org.skhuconnect.report.entity.ReportReasonType.ABUSE, "욕설이 포함된 글이라 신고합니다.");
        report.process(org.skhuconnect.report.entity.ReportStatus.ACTION_TAKEN,
                org.skhuconnect.report.entity.ReportActionType.HIDE,
                mock(org.skhuconnect.admin.entity.Admin.class), "확인", java.time.LocalDateTime.now());

        service.onReportProcessed(report);

        verify(notifications, times(2)).saveAndFlush(any(Notification.class));
    }
    private User user(long id){ User u=mock(User.class); when(u.getId()).thenReturn(id); when(u.isNotificationEnabled()).thenReturn(true); when(u.allows(any())).thenReturn(true); return u; }
}
