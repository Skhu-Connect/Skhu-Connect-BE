package org.skhuconnect.comment.service;

import org.skhuconnect.comment.dto.request.CommentCreateRequest;
import org.skhuconnect.comment.dto.request.CommentUpdateRequest;
import org.skhuconnect.comment.dto.response.CommentPageResponse;
import org.skhuconnect.comment.dto.response.CommentResponse;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.comment.exception.AnonymousNumberCollisionException;
import org.skhuconnect.comment.exception.CommentException;
import org.skhuconnect.comment.exception.CommentException.Reason;
import org.skhuconnect.comment.repository.CommentLikeCount;
import org.skhuconnect.comment.repository.CommentLikeRepository;
import org.skhuconnect.comment.repository.CommentRepository;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CommentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CommentCreationTransaction creationTransaction;
    private final AnonymousNumberRetryService retryService;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PetitionRepository petitionRepository;
    private final Clock clock;

    public CommentService(
            CommentCreationTransaction creationTransaction,
            AnonymousNumberRetryService retryService,
            CommentRepository commentRepository,
            CommentLikeRepository commentLikeRepository,
            PetitionRepository petitionRepository,
            Clock clock
    ) {
        this.creationTransaction = creationTransaction;
        this.retryService = retryService;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.petitionRepository = petitionRepository;
        this.clock = clock;
    }

    public CommentResponse create(
            Long userId,
            Long petitionId,
            CommentCreateRequest request
    ) {
        try {
            return creationTransaction.create(userId, petitionId, request.content());
        } catch (AnonymousNumberCollisionException exception) {
            return retryService.retryOnce(
                    userId, petitionId, request.content());
        }
    }

    @Transactional(readOnly = true)
    public CommentPageResponse findAll(
            Long userId,
            Long petitionId,
            int page,
            int size
    ) {
        validatePage(page, size);
        petitionRepository.findByIdAndDeletedFalseAndHiddenFalse(petitionId)
                .orElseThrow(() -> new CommentException(Reason.PETITION_NOT_FOUND));
        Page<Comment> comments = commentRepository.findByPetitionIdAndDeletedFalse(
                petitionId,
                PageRequest.of(page, size, Sort.by(
                        Sort.Order.asc("createdAt"), Sort.Order.asc("id")))
        );
        List<Long> commentIds = comments.getContent().stream()
                .map(Comment::getId)
                .toList();
        Map<Long, Long> likeCounts = findLikeCounts(commentIds);
        Set<Long> likedIds = findLikedIds(userId, commentIds);
        Page<CommentResponse> responses = comments.map(comment -> CommentResponse.from(
                comment,
                likeCounts.getOrDefault(comment.getId(), 0L),
                userId,
                likedIds.contains(comment.getId())
        ));
        return CommentPageResponse.from(responses);
    }

    @Transactional
    public CommentResponse update(
            Long userId,
            Long petitionId,
            Long commentId,
            CommentUpdateRequest request
    ) {
        Comment comment = findComment(petitionId, commentId);
        validateOwner(comment, userId);
        if (comment.isHidden()) {
            throw new CommentException(Reason.COMMENT_NOT_EDITABLE);
        }
        comment.update(request.content());
        return CommentResponse.from(
                comment,
                commentLikeRepository.countByCommentId(commentId),
                userId,
                commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)
        );
    }

    @Transactional
    public void delete(Long userId, Long petitionId, Long commentId) {
        Comment comment = findComment(petitionId, commentId);
        validateOwner(comment, userId);
        comment.delete(LocalDateTime.now(clock));
    }

    private Comment findComment(Long petitionId, Long commentId) {
        return commentRepository.findByIdAndPetitionIdAndDeletedFalse(commentId, petitionId)
                .orElseThrow(() -> new CommentException(Reason.COMMENT_NOT_FOUND));
    }

    private void validateOwner(Comment comment, Long userId) {
        if (!comment.isWrittenBy(userId)) {
            throw new CommentException(Reason.COMMENT_FORBIDDEN);
        }
    }

    private Map<Long, Long> findLikeCounts(List<Long> commentIds) {
        Map<Long, Long> counts = new HashMap<>();
        if (!commentIds.isEmpty()) {
            for (CommentLikeCount count : commentLikeRepository.countByCommentIds(commentIds)) {
                counts.put(count.getCommentId(), count.getLikeCount());
            }
        }
        return counts;
    }

    private Set<Long> findLikedIds(Long userId, List<Long> commentIds) {
        if (userId == null || commentIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(commentLikeRepository.findLikedCommentIds(userId, commentIds));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new CommentException(Reason.INVALID_PAGE);
        }
    }
}
