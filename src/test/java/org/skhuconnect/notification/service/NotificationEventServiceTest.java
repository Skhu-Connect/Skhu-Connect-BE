package org.skhuconnect.notification.service;

import org.junit.jupiter.api.*;
import org.skhuconnect.agreement.repository.AgreementRepository;
import org.skhuconnect.agreement.entity.Agreement;
import org.skhuconnect.comment.entity.*;
import org.skhuconnect.notification.entity.Notification;
import org.skhuconnect.notification.entity.NotificationType;
import org.skhuconnect.notification.fcm.FcmPushService;
import org.skhuconnect.notification.repository.NotificationRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationEventServiceTest {
    NotificationRepository notifications; AgreementRepository agreements; FcmPushService fcm; NotificationEventService service;
    @BeforeEach void setUp(){ notifications=mock(NotificationRepository.class); agreements=mock(AgreementRepository.class);
        fcm=mock(FcmPushService.class); service=new NotificationEventService(notifications,agreements,fcm); }
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
    @Test void petitionCommentNotifiesPetitionWriterButNotSelfOrReply() {
        User writer=user(1L), actor=user(2L); Petition petition=mock(Petition.class);
        when(petition.getId()).thenReturn(10L); when(petition.getWriter()).thenReturn(writer);
        PetitionAnonymousNumber writerMap=PetitionAnonymousNumber.create(petition,writer,1);
        PetitionAnonymousNumber actorMap=PetitionAnonymousNumber.create(petition,actor,2);
        Comment comment=Comment.create(petition,actor,actorMap,"comment"); ReflectionTestUtils.setField(comment,"id",20L);

        service.onPetitionCommentCreated(comment);

        verify(notifications).saveAndFlush(argThat(notification ->
                notification.getType()==NotificationType.PETITION_COMMENT_CREATED
                        && notification.getReceiver()==writer
                        && notification.getPetition()==petition
                        && notification.getComment()==comment));

        Comment selfComment=Comment.create(petition,writer,writerMap,"self"); ReflectionTestUtils.setField(selfComment,"id",21L);
        Comment reply=Comment.create(petition,actor,actorMap,comment,"reply"); ReflectionTestUtils.setField(reply,"id",22L);
        service.onPetitionCommentCreated(selfComment); service.onPetitionCommentCreated(reply);
        verify(notifications,times(1)).saveAndFlush(any(Notification.class));
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
    private User user(long id){ User u=mock(User.class); when(u.getId()).thenReturn(id); when(u.isNotificationEnabled()).thenReturn(true); return u; }
}
