package org.skhuconnect.comment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.comment.entity.PetitionAnonymousNumber;
import org.skhuconnect.comment.exception.CommentException;
import org.skhuconnect.comment.repository.CommentRepository;
import org.skhuconnect.comment.repository.PetitionAnonymousNumberRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.entity.PetitionStatus;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentCreationTransactionTest {

    private CommentRepository commentRepository;
    private PetitionAnonymousNumberRepository mappingRepository;
    private PetitionRepository petitionRepository;
    private UserRepository userRepository;
    private CommentCreationTransaction transaction;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        mappingRepository = mock(PetitionAnonymousNumberRepository.class);
        petitionRepository = mock(PetitionRepository.class);
        userRepository = mock(UserRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-06T03:00:00Z"),
                ZoneId.of("Asia/Seoul"));
        now = LocalDateTime.of(2026, 8, 6, 12, 0);
        transaction = new CommentCreationTransaction(commentRepository,
                mappingRepository, petitionRepository, userRepository, clock);
        when(commentRepository.saveAndFlush(any(Comment.class)))
                .thenAnswer(invocation -> {
                    Comment comment = invocation.getArgument(0);
                    ReflectionTestUtils.setField(comment, "id", 100L);
                    return comment;
                });
    }

    @Test
    void firstCommentLocksPetitionAndAllocatesMaxPlusOne() {
        User user = user(1L);
        Petition petition = petition(user, PetitionStatus.OPEN, now.minusDays(1));
        when(petitionRepository.findVisibleByIdForUpdate(10L))
                .thenReturn(Optional.of(petition));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(mappingRepository.findByPetitionIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());
        when(mappingRepository.findMaxAnonymousNumberByPetitionId(10L))
                .thenReturn(Optional.of(2));
        when(mappingRepository.saveAndFlush(any(PetitionAnonymousNumber.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = transaction.create(1L, 10L, "content");

        assertThat(response.anonymousNumber()).isEqualTo(3);
        assertThat(response.myComment()).isTrue();
        verify(petitionRepository).findVisibleByIdForUpdate(10L);
        verify(mappingRepository).saveAndFlush(any(PetitionAnonymousNumber.class));
    }

    @Test
    void existingMappingIsReusedAfterPreviousCommentDeletion() {
        User user = user(1L);
        Petition petition = petition(user, PetitionStatus.ANSWERED, now.minusDays(40));
        PetitionAnonymousNumber mapping = PetitionAnonymousNumber.create(petition, user, 7);
        when(petitionRepository.findVisibleByIdForUpdate(10L))
                .thenReturn(Optional.of(petition));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(mappingRepository.findByPetitionIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(mapping));

        var response = transaction.create(1L, 10L, "new comment");

        assertThat(response.anonymousNumber()).isEqualTo(7);
        verify(mappingRepository, never()).findMaxAnonymousNumberByPetitionId(10L);
        verify(mappingRepository, never()).saveAndFlush(any());
    }

    @Test
    void underReviewAndAnsweredAllowCommentsButExpiredRejects() {
        assertAllowed(PetitionStatus.UNDER_REVIEW);
        assertAllowed(PetitionStatus.ANSWERED);

        User user = user(1L);
        Petition expired = petition(user, PetitionStatus.EXPIRED, now.minusDays(40));
        when(petitionRepository.findVisibleByIdForUpdate(10L))
                .thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> transaction.create(1L, 10L, "content"))
                .isInstanceOf(CommentException.class)
                .extracting("reason")
                .isEqualTo(CommentException.Reason.PETITION_NOT_COMMENTABLE);
    }

    private void assertAllowed(PetitionStatus status) {
        User user = user(1L);
        Petition petition = petition(user, status, now.minusDays(40));
        PetitionAnonymousNumber mapping = PetitionAnonymousNumber.create(petition, user, 1);
        when(petitionRepository.findVisibleByIdForUpdate(10L))
                .thenReturn(Optional.of(petition));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(mappingRepository.findByPetitionIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(mapping));
        assertThat(transaction.create(1L, 10L, "content")).isNotNull();
    }

    private Petition petition(User writer, PetitionStatus status, LocalDateTime createdAt) {
        Petition petition = Petition.create(writer, PetitionCategory.FACILITY,
                "title", "content", 10, createdAt);
        ReflectionTestUtils.setField(petition, "id", 10L);
        ReflectionTestUtils.setField(petition, "status", status);
        return petition;
    }

    private User user(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }
}
