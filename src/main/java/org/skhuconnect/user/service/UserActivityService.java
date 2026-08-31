package org.skhuconnect.user.service;

import org.skhuconnect.agreement.entity.Agreement;
import org.skhuconnect.agreement.repository.AgreementRepository;
import org.skhuconnect.bookmark.entity.Bookmark;
import org.skhuconnect.bookmark.repository.BookmarkRepository;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.comment.repository.CommentLikeCount;
import org.skhuconnect.comment.repository.CommentLikeRepository;
import org.skhuconnect.comment.repository.CommentRepository;
import org.skhuconnect.notification.dto.NotificationPageResponse;
import org.skhuconnect.notification.service.NotificationService;
import org.skhuconnect.petition.dto.response.PetitionPageResponse;
import org.skhuconnect.petition.dto.response.PetitionQueryResponse;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.dto.UserCommentPageResponse;
import org.skhuconnect.user.dto.UserCommentResponse;
import org.skhuconnect.user.dto.UserMeResponse;
import org.skhuconnect.user.dto.NotificationSettingsResponse;
import org.skhuconnect.user.dto.NotificationSettingsUpdateRequest;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.exception.UserActivityException;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserActivityService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final PetitionRepository petitionRepository;
    private final AgreementRepository agreementRepository;
    private final BookmarkRepository bookmarkRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    public UserActivityService(
            UserRepository userRepository,
            PetitionRepository petitionRepository,
            AgreementRepository agreementRepository,
            BookmarkRepository bookmarkRepository,
            CommentRepository commentRepository,
            CommentLikeRepository commentLikeRepository,
            NotificationService notificationService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.petitionRepository = petitionRepository;
        this.agreementRepository = agreementRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserMeResponse findMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserActivityException(
                        UserActivityException.Reason.USER_NOT_FOUND));
        return UserMeResponse.from(user);
    }

    @Transactional
    public NotificationSettingsResponse updateNotificationSettings(
            Long userId,
            NotificationSettingsUpdateRequest request
    ) {
        if (request.isEmpty()) {
            throw new UserActivityException(
                    UserActivityException.Reason.INVALID_NOTIFICATION_SETTINGS);
        }
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserActivityException(
                        UserActivityException.Reason.USER_NOT_FOUND));
        user.changeNotificationSettings(
                request.agreement(),
                request.answer(),
                request.reply(),
                request.like(),
                request.notice(),
                request.report()
        );
        return NotificationSettingsResponse.from(user);
    }

    @Transactional(readOnly = true)
    public PetitionPageResponse findMyPetitions(Long userId, int page, int size) {
        validatePage(page, size);
        return mapPetitions(petitionRepository.findVisibleByWriterId(
                userId, newestPage(page, size)));
    }

    @Transactional(readOnly = true)
    public PetitionPageResponse findMyAgreements(Long userId, int page, int size) {
        validatePage(page, size);
        return mapPetitions(agreementRepository
                .findVisibleByUserId(userId, newestPage(page, size))
                .map(Agreement::getPetition));
    }

    @Transactional(readOnly = true)
    public PetitionPageResponse findMyBookmarks(Long userId, int page, int size) {
        validatePage(page, size);
        return mapPetitions(bookmarkRepository
                .findVisibleByUserId(userId, newestPage(page, size))
                .map(Bookmark::getPetition));
    }

    @Transactional(readOnly = true)
    public UserCommentPageResponse findMyComments(Long userId, int page, int size) {
        validatePage(page, size);
        Page<Comment> comments = commentRepository.findVisibleActivityByWriterId(
                userId, newestPage(page, size));
        List<Long> ids = comments.getContent().stream().map(Comment::getId).toList();
        Map<Long, Long> counts = findLikeCounts(ids);
        Set<Long> likedIds = ids.isEmpty()
                ? Set.of()
                : Set.copyOf(commentLikeRepository.findLikedCommentIds(userId, ids));
        return UserCommentPageResponse.from(comments.map(comment ->
                UserCommentResponse.from(
                        comment,
                        counts.getOrDefault(comment.getId(), 0L),
                        userId,
                        likedIds.contains(comment.getId())
                )));
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse findMyNotifications(
            Long userId,
            int page,
            int size
    ) {
        validatePage(page, size);
        return notificationService.findAll(userId, page, size);
    }

    private PetitionPageResponse mapPetitions(Page<Petition> petitions) {
        LocalDateTime now = LocalDateTime.now(clock);
        return PetitionPageResponse.from(petitions.map(
                petition -> PetitionQueryResponse.from(petition, now)));
    }

    private Map<Long, Long> findLikeCounts(List<Long> ids) {
        Map<Long, Long> result = new HashMap<>();
        if (!ids.isEmpty()) {
            for (CommentLikeCount count : commentLikeRepository.countByCommentIds(ids)) {
                result.put(count.getCommentId(), count.getLikeCount());
            }
        }
        return result;
    }

    private Pageable newestPage(int page, int size) {
        return PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        ));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new UserActivityException(
                    UserActivityException.Reason.INVALID_PAGE);
        }
    }
}
