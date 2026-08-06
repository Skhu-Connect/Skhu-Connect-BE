package org.skhuconnect.comment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.comment.dto.request.CommentCreateRequest;
import org.skhuconnect.comment.dto.request.CommentUpdateRequest;
import org.skhuconnect.comment.dto.response.CommentResponse;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.comment.entity.PetitionAnonymousNumber;
import org.skhuconnect.comment.exception.AnonymousNumberCollisionException;
import org.skhuconnect.comment.exception.CommentException;
import org.skhuconnect.comment.repository.CommentLikeRepository;
import org.skhuconnect.comment.repository.CommentRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.entity.User;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentServiceTest {

    private CommentCreationTransaction creationTransaction;
    private AnonymousNumberRetryService retryService;
    private CommentRepository commentRepository;
    private CommentLikeRepository likeRepository;
    private PetitionRepository petitionRepository;
    private CommentService service;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        creationTransaction = mock(CommentCreationTransaction.class);
        retryService = mock(AnonymousNumberRetryService.class);
        commentRepository = mock(CommentRepository.class);
        likeRepository = mock(CommentLikeRepository.class);
        petitionRepository = mock(PetitionRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-06T03:00:00Z"),
                ZoneId.of("Asia/Seoul"));
        now = LocalDateTime.of(2026, 8, 6, 12, 0);
        service = new CommentService(creationTransaction, retryService, commentRepository,
                likeRepository, petitionRepository, clock);
    }

    @Test
    void collisionRetriesExactlyOnceWithNewTransactionMethod() {
        CommentCreateRequest request = new CommentCreateRequest("content");
        doThrow(new AnonymousNumberCollisionException(new RuntimeException()))
                .when(creationTransaction).create(1L, 10L, "content");
        CommentResponse expected = new CommentResponse(
                1L, "content", 1, 0, true, false, false, now, now);
        when(retryService.retryOnce(1L, 10L, "content"))
                .thenReturn(expected);

        assertThat(service.create(1L, 10L, request)).isSameAs(expected);
        verify(retryService).retryOnce(1L, 10L, "content");
    }

    @Test
    void anonymousListHasFalseUserFlagsAndExcludesDeletedThroughRepository() {
        Comment comment = comment(1L, 10L, false);
        when(petitionRepository.findByIdAndDeletedFalseAndHiddenFalse(10L))
                .thenReturn(Optional.of(comment.getPetition()));
        when(commentRepository.findRootPage(any(Long.class), any()))
                .thenReturn(new PageImpl<>(List.of(comment)));

        var response = service.findAll(null, 10L, 0, 20);

        assertThat(response.content()).singleElement().satisfies(found -> {
            assertThat(found.anonymousNumber()).isEqualTo(1);
            assertThat(found.myComment()).isFalse();
            assertThat(found.liked()).isFalse();
        });
    }

    @Test
    void hiddenCommentReturnsNoticeInsteadOfOriginal() {
        Comment comment = comment(1L, 10L, true);
        when(petitionRepository.findByIdAndDeletedFalseAndHiddenFalse(10L))
                .thenReturn(Optional.of(comment.getPetition()));
        when(commentRepository.findRootPage(any(Long.class), any()))
                .thenReturn(new PageImpl<>(List.of(comment)));

        var found = service.findAll(null, 10L, 0, 20).content().get(0);

        assertThat(found.content()).isEqualTo("관리자에 의해 숨김 처리된 댓글입니다.");
        assertThat(found.hidden()).isTrue();
    }

    @Test
    void ownerCanUpdateAndDeleteButOtherUserCannot() {
        Comment comment = comment(1L, 10L, false);
        when(commentRepository.findByIdAndPetitionIdAndDeletedFalse(5L, 10L))
                .thenReturn(Optional.of(comment));

        var updated = service.update(1L, 10L, 5L, new CommentUpdateRequest("updated"));
        service.delete(1L, 10L, 5L);

        assertThat(updated.content()).isEqualTo("updated");
        assertThat(comment.isDeleted()).isTrue();

        Comment otherComment = comment(1L, 10L, false);
        when(commentRepository.findByIdAndPetitionIdAndDeletedFalse(6L, 10L))
                .thenReturn(Optional.of(otherComment));
        assertThatThrownBy(() -> service.update(
                2L, 10L, 6L, new CommentUpdateRequest("denied")))
                .isInstanceOf(CommentException.class)
                .extracting("reason")
                .isEqualTo(CommentException.Reason.COMMENT_FORBIDDEN);
    }

    @Test
    void validatesPageBounds() {
        assertThatThrownBy(() -> service.findAll(null, 10L, -1, 20))
                .isInstanceOf(CommentException.class)
                .extracting("reason").isEqualTo(CommentException.Reason.INVALID_PAGE);
        assertThatThrownBy(() -> service.findAll(null, 10L, 0, 101))
                .isInstanceOf(CommentException.class)
                .extracting("reason").isEqualTo(CommentException.Reason.INVALID_PAGE);
    }

    private Comment comment(Long writerId, Long petitionId, boolean hidden) {
        User writer = mock(User.class);
        when(writer.getId()).thenReturn(writerId);
        Petition petition = mock(Petition.class);
        when(petition.getId()).thenReturn(petitionId);
        PetitionAnonymousNumber mapping = PetitionAnonymousNumber.create(
                petition, writer, 1);
        Comment comment = Comment.create(petition, writer, mapping, "original");
        ReflectionTestUtils.setField(comment, "id", 5L);
        ReflectionTestUtils.setField(comment, "hidden", hidden);
        ReflectionTestUtils.setField(comment, "createdAt", now);
        ReflectionTestUtils.setField(comment, "updatedAt", now);
        return comment;
    }
}
