package org.skhuconnect.comment.service;

import org.skhuconnect.comment.dto.response.CommentLikeResponse;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.comment.entity.CommentLike;
import org.skhuconnect.comment.exception.CommentException;
import org.skhuconnect.comment.exception.CommentException.Reason;
import org.skhuconnect.comment.repository.CommentLikeRepository;
import org.skhuconnect.comment.repository.CommentRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentLikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public CommentLikeService(
            CommentLikeRepository commentLikeRepository,
            CommentRepository commentRepository,
            UserRepository userRepository
    ) {
        this.commentLikeRepository = commentLikeRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CommentLikeResponse like(Long userId, Long petitionId, Long commentId) {
        Comment comment = findLikeableComment(petitionId, commentId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommentException(Reason.USER_NOT_FOUND));
        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            throw new CommentException(Reason.COMMENT_LIKE_DUPLICATE);
        }
        try {
            commentLikeRepository.saveAndFlush(CommentLike.create(comment, user));
        } catch (DataIntegrityViolationException exception) {
            throw new CommentException(Reason.COMMENT_LIKE_DUPLICATE);
        }
        return new CommentLikeResponse(
                commentId, commentLikeRepository.countByCommentId(commentId), true);
    }

    @Transactional
    public CommentLikeResponse cancel(Long userId, Long petitionId, Long commentId) {
        Comment comment = findLikeableComment(petitionId, commentId);
        CommentLike commentLike = commentLikeRepository
                .findByCommentIdAndUserId(commentId, userId)
                .orElseThrow(() -> new CommentException(Reason.COMMENT_LIKE_NOT_FOUND));
        commentLikeRepository.delete(commentLike);
        commentLikeRepository.flush();
        return new CommentLikeResponse(
                commentId, commentLikeRepository.countByCommentId(commentId), false);
    }

    private Comment findLikeableComment(Long petitionId, Long commentId) {
        Comment comment = commentRepository
                .findByIdAndPetitionIdAndDeletedFalse(commentId, petitionId)
                .orElseThrow(() -> new CommentException(Reason.COMMENT_NOT_FOUND));
        if (comment.getPetition().isHidden() || comment.getPetition().isDeleted()
                || comment.isHidden()) {
            throw new CommentException(Reason.COMMENT_NOT_LIKEABLE);
        }
        return comment;
    }
}
