package org.skhuconnect.comment.service;

import org.junit.jupiter.api.Test;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.comment.entity.PetitionAnonymousNumber;
import org.skhuconnect.comment.exception.CommentException;
import org.skhuconnect.comment.repository.CommentRepository;
import org.skhuconnect.comment.repository.PetitionAnonymousNumberRepository;
import org.skhuconnect.notification.service.NotificationEventService;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnonymousNumberRetryServiceTest {

    @Test
    void retryMethodRequiresNewTransactionAndDoesNotAllocateAnotherNumber()
            throws Exception {
        Transactional transactional = AnonymousNumberRetryService.class
                .getMethod("retryOnce", Long.class, Long.class, String.class)
                .getAnnotation(Transactional.class);
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);

        CommentRepository commentRepository = mock(CommentRepository.class);
        PetitionAnonymousNumberRepository mappingRepository =
                mock(PetitionAnonymousNumberRepository.class);
        PetitionRepository petitionRepository = mock(PetitionRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        Petition petition = mock(Petition.class);
        User user = mock(User.class);
        when(petitionRepository.findVisibleByIdForUpdate(10L))
                .thenReturn(Optional.of(petition));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(mappingRepository.findByPetitionIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());
        when(petition.getStatus()).thenReturn(
                org.skhuconnect.petition.entity.PetitionStatus.ANSWERED);
        AnonymousNumberRetryService service = new AnonymousNumberRetryService(
                commentRepository, mappingRepository, petitionRepository,
                userRepository, Clock.systemUTC());

        assertThatThrownBy(() -> service.retryOnce(1L, 10L, "content"))
                .isInstanceOf(CommentException.class)
                .extracting("reason")
                .isEqualTo(CommentException.Reason.ANONYMOUS_NUMBER_CONFLICT);
    }

    @Test
    void retryRootCommentTriggersPetitionCommentNotification() {
        CommentRepository commentRepository = mock(CommentRepository.class);
        PetitionAnonymousNumberRepository mappingRepository =
                mock(PetitionAnonymousNumberRepository.class);
        PetitionRepository petitionRepository = mock(PetitionRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        NotificationEventService notificationEventService =
                mock(NotificationEventService.class);
        Petition petition = mock(Petition.class);
        User writer = mock(User.class);
        User commenter = mock(User.class);
        when(writer.getId()).thenReturn(1L);
        when(commenter.getId()).thenReturn(2L);
        when(petition.getId()).thenReturn(10L);
        when(petition.getWriter()).thenReturn(writer);
        when(petition.getStatus()).thenReturn(
                org.skhuconnect.petition.entity.PetitionStatus.ANSWERED);
        PetitionAnonymousNumber mapping = PetitionAnonymousNumber.create(
                petition, commenter, 2);
        when(petitionRepository.findVisibleByIdForUpdate(10L))
                .thenReturn(Optional.of(petition));
        when(userRepository.findById(2L)).thenReturn(Optional.of(commenter));
        when(mappingRepository.findByPetitionIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(mapping));
        when(commentRepository.saveAndFlush(any(Comment.class)))
                .thenAnswer(invocation -> {
                    Comment comment = invocation.getArgument(0);
                    ReflectionTestUtils.setField(comment, "id", 100L);
                    return comment;
                });
        AnonymousNumberRetryService service = new AnonymousNumberRetryService(
                commentRepository, mappingRepository, petitionRepository,
                userRepository, Clock.systemUTC());
        service.setNotificationEventService(notificationEventService);

        service.retryOnce(2L, 10L, "content");

        verify(notificationEventService).onPetitionCommentCreated(any(Comment.class));
    }
}
