package org.skhuconnect.comment.service;

import org.skhuconnect.comment.dto.response.CommentResponse;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.comment.entity.PetitionAnonymousNumber;
import org.skhuconnect.comment.exception.AnonymousNumberCollisionException;
import org.skhuconnect.comment.exception.CommentException;
import org.skhuconnect.comment.exception.CommentException.Reason;
import org.skhuconnect.comment.repository.CommentRepository;
import org.skhuconnect.comment.repository.PetitionAnonymousNumberRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionStatus;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.skhuconnect.notification.service.NotificationEventService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class CommentCreationTransaction {

    private final CommentRepository commentRepository;
    private final PetitionAnonymousNumberRepository anonymousNumberRepository;
    private final PetitionRepository petitionRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private NotificationEventService notificationEventService;

    public CommentCreationTransaction(
            CommentRepository commentRepository,
            PetitionAnonymousNumberRepository anonymousNumberRepository,
            PetitionRepository petitionRepository,
            UserRepository userRepository,
            Clock clock
    ) {
        this.commentRepository = commentRepository;
        this.anonymousNumberRepository = anonymousNumberRepository;
        this.petitionRepository = petitionRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Autowired
    void setNotificationEventService(NotificationEventService service) {
        this.notificationEventService = service;
    }

    @Transactional
    public CommentResponse create(Long userId, Long petitionId, String content) {
        return create(userId, petitionId, content, null);
    }

    @Transactional
    public CommentResponse create(Long userId, Long petitionId, String content, Long parentCommentId) {
        Petition petition = findLockedPetition(petitionId);
        validateCommentable(petition);
        User user = findUser(userId);
        PetitionAnonymousNumber mapping = anonymousNumberRepository
                .findByPetitionIdAndUserId(petitionId, userId)
                .orElseGet(() -> createMapping(petition, user));
        Comment parent = findParent(petitionId, parentCommentId);
        Comment comment = commentRepository.saveAndFlush(
                Comment.create(petition, user, mapping, parent, content));
        if (notificationEventService != null) {
            if (comment.isReply()) {
                notificationEventService.onReplyCreated(comment);
            } else {
                notificationEventService.onPetitionCommentCreated(comment);
            }
        }
        return CommentResponse.from(comment, 0, userId, false);
    }


    private Comment findParent(Long petitionId, Long parentCommentId) {
        if (parentCommentId == null) return null;
        Comment parent = commentRepository.findByIdAndPetitionId(parentCommentId, petitionId)
                .orElseThrow(() -> new CommentException(Reason.PARENT_COMMENT_NOT_FOUND));
        if (parent.isReply()) throw new CommentException(Reason.REPLY_DEPTH_EXCEEDED);
        if (parent.isDeleted()) throw new CommentException(Reason.PARENT_COMMENT_DELETED);
        if (parent.isHidden()) throw new CommentException(Reason.PARENT_COMMENT_HIDDEN);
        return parent;
    }

    private PetitionAnonymousNumber createMapping(Petition petition, User user) {
        int nextNumber = anonymousNumberRepository
                .findMaxAnonymousNumberByPetitionId(petition.getId())
                .orElse(0) + 1;
        try {
            return anonymousNumberRepository.saveAndFlush(
                    PetitionAnonymousNumber.create(petition, user, nextNumber));
        } catch (DataIntegrityViolationException exception) {
            throw new AnonymousNumberCollisionException(exception);
        }
    }

    private Petition findLockedPetition(Long petitionId) {
        return petitionRepository.findVisibleByIdForUpdate(petitionId)
                .orElseThrow(() -> new CommentException(Reason.PETITION_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CommentException(Reason.USER_NOT_FOUND));
    }

    private void validateCommentable(Petition petition) {
        PetitionStatus status = petition.getStatus();
        LocalDateTime now = LocalDateTime.now(clock);
        boolean expiredOpen = status == PetitionStatus.OPEN
                && !petition.getAgreementDeadline().isAfter(now);
        if (status == PetitionStatus.EXPIRED || expiredOpen) {
            throw new CommentException(Reason.PETITION_NOT_COMMENTABLE);
        }
    }
}
