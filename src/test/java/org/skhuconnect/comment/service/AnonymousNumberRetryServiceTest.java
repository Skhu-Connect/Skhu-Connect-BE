package org.skhuconnect.comment.service;

import org.junit.jupiter.api.Test;
import org.skhuconnect.comment.exception.CommentException;
import org.skhuconnect.comment.repository.CommentRepository;
import org.skhuconnect.comment.repository.PetitionAnonymousNumberRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
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
}
