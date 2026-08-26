package org.skhuconnect.notification.service;

import org.skhuconnect.agreement.entity.Agreement;
import org.skhuconnect.agreement.repository.AgreementRepository;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.notification.entity.*;
import org.skhuconnect.notification.fcm.FcmPushService;
import org.skhuconnect.notification.repository.NotificationRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.user.entity.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import static org.skhuconnect.notification.entity.NotificationType.*;

@Service
public class NotificationEventService {
    private final NotificationRepository notifications;
    private final AgreementRepository agreements;
    private final ApplicationEventPublisher events;
    public NotificationEventService(NotificationRepository notifications, AgreementRepository agreements, ApplicationEventPublisher events) {
        this.notifications = notifications; this.agreements = agreements; this.events = events;
    }

    public void onAgreementAdded(Petition petition, int previousCount) {
        int current = petition.getAgreementCount();
        int target = petition.getTargetAgreementCount();
        User writer = petition.getWriter();
        if ((long) previousCount * 100 < (long) target * 60
                && (long) current * 100 >= (long) target * 60) {
            createOnce(writer, PETITION_AGREEMENT_60_PERCENT, petition, null,
                    "petition:60:" + petition.getId() + ":" + writer.getId());
        }
        if (previousCount < target && current >= target) {
            createOnce(writer, PETITION_AGREEMENT_100_PERCENT, petition, null,
                    "petition:100:" + petition.getId() + ":" + writer.getId());
            notifyPetitionAudience(petition, PETITION_UNDER_REVIEW, "petition:review:");
        }
    }

    public void onNoticePublished(Long noticeId, String noticeTitle, Iterable<User> receivers) {
        String title = "새 공지사항이 등록되었습니다";
        for (User receiver : receivers) {
            createNoticeOnce(receiver, title, noticeTitle, "notice:published:" + noticeId + ":" + receiver.getId());
        }
    }

    public void onPetitionAnswered(Petition petition) {
        notifyPetitionAudience(petition, PETITION_ANSWERED, "petition:answered:");
    }

    public void onCommentLiked(Comment comment, User actor) {
        User receiver = comment.getWriter();
        if (receiver.getId().equals(actor.getId())) return;
        NotificationType type = comment.isReply() ? REPLY_LIKE : COMMENT_LIKE;
        createOnce(receiver, type, comment.getPetition(), comment,
                "comment:like:" + comment.getId() + ":" + actor.getId() + ":" + receiver.getId());
    }

    public void onReplyCreated(Comment reply) {
        if (!reply.isReply()) return;
        User receiver = reply.getParentComment().getWriter();
        if (receiver.getId().equals(reply.getWriter().getId())) return;
        createOnce(receiver, COMMENT_REPLY, reply.getPetition(), reply,
                "comment:reply:" + reply.getId() + ":" + receiver.getId());
    }

    private void notifyPetitionAudience(Petition petition, NotificationType type, String prefix) {
        User writer = petition.getWriter();
        createOnce(writer, type, petition, null, prefix + petition.getId() + ":" + writer.getId());
        for (Agreement agreement : agreements.findByPetitionId(petition.getId())) {
            User receiver = agreement.getUser();
            if (!receiver.getId().equals(writer.getId())) {
                createOnce(receiver, type, petition, null,
                        prefix + petition.getId() + ":" + receiver.getId());
            }
        }
    }

    private void createNoticeOnce(User receiver, String title, String body, String eventKey) {
        if (!receiver.isNotificationEnabled()
                || !receiver.allows(NOTICE.point())
                || notifications.existsByEventKey(eventKey)) return;
        try {
            Notification notification = notifications.saveAndFlush(Notification.createNotice(receiver, title, body, eventKey));
            events.publishEvent(FcmPushService.PushMessage.from(notification));
        } catch (DataIntegrityViolationException ignored) { }
    }

    private void createOnce(User receiver, NotificationType type, Petition petition,
                            Comment comment, String eventKey) {
        if (!receiver.isNotificationEnabled()
                || !receiver.allows(type.point())
                || notifications.existsByEventKey(eventKey)) return;
        try {
            Notification notification = notifications.saveAndFlush(Notification.create(receiver, type, petition, comment, eventKey));
            events.publishEvent(FcmPushService.PushMessage.from(notification));
        } catch (DataIntegrityViolationException ignored) {
            // Unique event_key is the final guard for concurrent duplicate events.
        }
    }
}
