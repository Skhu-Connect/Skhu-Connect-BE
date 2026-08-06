package org.skhuconnect.comment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.comment.entity.CommentLike;
import org.skhuconnect.comment.exception.CommentException;
import org.skhuconnect.comment.repository.CommentLikeRepository;
import org.skhuconnect.comment.repository.CommentRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentLikeServiceTest {

    private CommentLikeRepository likeRepository;
    private CommentRepository commentRepository;
    private UserRepository userRepository;
    private CommentLikeService service;

    @BeforeEach
    void setUp() {
        likeRepository = mock(CommentLikeRepository.class);
        commentRepository = mock(CommentRepository.class);
        userRepository = mock(UserRepository.class);
        service = new CommentLikeService(likeRepository, commentRepository, userRepository);
    }

    @Test
    void answeredAndExpiredPetitionCommentsCanBeLikedAndCanceled() {
        for (org.skhuconnect.petition.entity.PetitionStatus status : List.of(
                org.skhuconnect.petition.entity.PetitionStatus.ANSWERED,
                org.skhuconnect.petition.entity.PetitionStatus.EXPIRED)) {
            Comment comment = comment(status, false, false, false);
            User user = mock(User.class);
            when(commentRepository.findByIdAndPetitionIdAndDeletedFalse(5L, 10L))
                    .thenReturn(Optional.of(comment));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(likeRepository.countByCommentId(5L)).thenReturn(1L, 0L);

            assertThat(service.like(1L, 10L, 5L).liked()).isTrue();
            CommentLike like = CommentLike.create(comment, user);
            when(likeRepository.findByCommentIdAndUserId(5L, 1L))
                    .thenReturn(Optional.of(like));
            assertThat(service.cancel(1L, 10L, 5L).liked()).isFalse();

        }
        verify(likeRepository, org.mockito.Mockito.times(2))
                .saveAndFlush(any(CommentLike.class));
    }

    @Test
    void duplicateLikeReturnsConflictReason() {
        Comment comment = comment(
                org.skhuconnect.petition.entity.PetitionStatus.OPEN, false, false, false);
        when(commentRepository.findByIdAndPetitionIdAndDeletedFalse(5L, 10L))
                .thenReturn(Optional.of(comment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(likeRepository.existsByCommentIdAndUserId(5L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.like(1L, 10L, 5L))
                .isInstanceOf(CommentException.class)
                .extracting("reason")
                .isEqualTo(CommentException.Reason.COMMENT_LIKE_DUPLICATE);
    }

    @Test
    void concurrentDatabaseDuplicateIsConvertedToDuplicateReason() {
        Comment comment = comment(
                org.skhuconnect.petition.entity.PetitionStatus.OPEN, false, false, false);
        when(commentRepository.findByIdAndPetitionIdAndDeletedFalse(5L, 10L))
                .thenReturn(Optional.of(comment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException(
                "duplicate"))
                .when(likeRepository).saveAndFlush(any(CommentLike.class));

        assertThatThrownBy(() -> service.like(1L, 10L, 5L))
                .isInstanceOf(CommentException.class)
                .extracting("reason")
                .isEqualTo(CommentException.Reason.COMMENT_LIKE_DUPLICATE);
    }
    @Test
    void hiddenOrDeletedPetitionAndHiddenCommentCannotBeLiked() {
        for (Comment comment : List.of(
                comment(org.skhuconnect.petition.entity.PetitionStatus.OPEN, true, false, false),
                comment(org.skhuconnect.petition.entity.PetitionStatus.OPEN, false, true, false),
                comment(org.skhuconnect.petition.entity.PetitionStatus.OPEN, false, false, true))) {
            when(commentRepository.findByIdAndPetitionIdAndDeletedFalse(5L, 10L))
                    .thenReturn(Optional.of(comment));
            assertThatThrownBy(() -> service.like(1L, 10L, 5L))
                    .isInstanceOf(CommentException.class)
                    .extracting("reason")
                    .isEqualTo(CommentException.Reason.COMMENT_NOT_LIKEABLE);
        }
    }

    private Comment comment(
            org.skhuconnect.petition.entity.PetitionStatus status,
            boolean petitionHidden,
            boolean petitionDeleted,
            boolean commentHidden
    ) {
        Comment comment = mock(Comment.class);
        Petition petition = mock(Petition.class);
        when(petition.getStatus()).thenReturn(status);
        when(petition.isHidden()).thenReturn(petitionHidden);
        when(petition.isDeleted()).thenReturn(petitionDeleted);
        when(comment.getPetition()).thenReturn(petition);
        when(comment.isHidden()).thenReturn(commentHidden);
        return comment;
    }
}
