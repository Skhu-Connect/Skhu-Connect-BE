package org.skhuconnect.agreement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.agreement.dto.response.AgreementResponse;
import org.skhuconnect.agreement.entity.Agreement;
import org.skhuconnect.agreement.exception.AgreementException;
import org.skhuconnect.agreement.repository.AgreementRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgreementServiceTest {

    private AgreementRepository agreementRepository;
    private PetitionRepository petitionRepository;
    private UserRepository userRepository;
    private AgreementService service;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        agreementRepository = mock(AgreementRepository.class);
        petitionRepository = mock(PetitionRepository.class);
        userRepository = mock(UserRepository.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-05T03:00:00Z"),
                ZoneId.of("Asia/Seoul"));
        now = LocalDateTime.of(2026, 8, 5, 12, 0);
        service = new AgreementService(
                agreementRepository, petitionRepository, userRepository, clock);
    }

    @Test
    void writerCanAgreeOwnPetition() {
        User writer = user(1L);
        Petition petition = petition(writer, 10);
        ReflectionTestUtils.setField(petition, "id", 10L);
        when(petitionRepository.findVisibleByIdForUpdate(10L))
                .thenReturn(Optional.of(petition));
        when(userRepository.findById(1L)).thenReturn(Optional.of(writer));

        AgreementResponse response = service.agree(1L, 10L);

        verify(agreementRepository).saveAndFlush(any(Agreement.class));
        assertThat(response.agreementCount()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(PetitionStatus.OPEN);
    }

    @Test
    void reachingTargetTransitionsToUnderReview() {
        Petition petition = petition(user(2L), 2);
        ReflectionTestUtils.setField(petition, "id", 10L);
        ReflectionTestUtils.setField(petition, "agreementCount", 1);
        when(petitionRepository.findVisibleByIdForUpdate(10L))
                .thenReturn(Optional.of(petition));
        User agreeingUser = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(agreeingUser));

        AgreementResponse response = service.agree(1L, 10L);

        assertThat(response.agreementCount()).isEqualTo(2);
        assertThat(response.status()).isEqualTo(PetitionStatus.UNDER_REVIEW);
        assertThat(petition.getReviewStartedAt()).isEqualTo(now);
    }

    @Test
    void duplicateAgreementIsRejectedWithoutCountChange() {
        Petition petition = petition(user(2L), 10);
        when(petitionRepository.findVisibleByIdForUpdate(10L))
                .thenReturn(Optional.of(petition));
        User agreeingUser = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(agreeingUser));
        when(agreementRepository.existsByPetitionIdAndUserId(10L, 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.agree(1L, 10L))
                .isInstanceOf(AgreementException.class)
                .extracting("reason")
                .isEqualTo(AgreementException.Reason.AGREEMENT_DUPLICATE);
        assertThat(petition.getAgreementCount()).isZero();
    }

    @Test
    void cancelDeletesAgreementAndDecrementsCount() {
        Petition petition = petition(user(2L), 10);
        ReflectionTestUtils.setField(petition, "agreementCount", 1);
        Agreement agreement = Agreement.create(petition, user(1L));
        when(petitionRepository.findVisibleByIdForUpdate(10L))
                .thenReturn(Optional.of(petition));
        when(agreementRepository.findByPetitionIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(agreement));

        service.cancel(1L, 10L);

        verify(agreementRepository).delete(agreement);
        assertThat(petition.getAgreementCount()).isZero();
    }

    @Test
    void nonOpenExpiredHiddenAndDeletedPetitionsAreRejected() {
        Petition nonOpen = petition(user(1L), 10);
        ReflectionTestUtils.setField(
                nonOpen, "status", PetitionStatus.UNDER_REVIEW);
        assertNotAgreeable(nonOpen);

        Petition expired = petition(user(1L), 10);
        ReflectionTestUtils.setField(expired, "agreementDeadline", now);
        assertNotAgreeable(expired);

        Petition hidden = petition(user(1L), 10);
        ReflectionTestUtils.setField(hidden, "hidden", true);
        assertNotAgreeable(hidden);

        when(petitionRepository.findVisibleByIdForUpdate(10L))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.agree(1L, 10L))
                .isInstanceOf(AgreementException.class)
                .extracting("reason")
                .isEqualTo(AgreementException.Reason.PETITION_NOT_FOUND);
    }

    @Test
    void missingAgreementCannotBeCanceled() {
        Petition petition = petition(user(2L), 10);
        when(petitionRepository.findVisibleByIdForUpdate(10L))
                .thenReturn(Optional.of(petition));
        when(agreementRepository.findByPetitionIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(1L, 10L))
                .isInstanceOf(AgreementException.class)
                .extracting("reason")
                .isEqualTo(AgreementException.Reason.AGREEMENT_NOT_FOUND);
    }

    private void assertNotAgreeable(Petition petition) {
        when(petitionRepository.findVisibleByIdForUpdate(10L))
                .thenReturn(Optional.of(petition));
        User agreeingUser = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(agreeingUser));

        assertThatThrownBy(() -> service.agree(1L, 10L))
                .isInstanceOf(AgreementException.class)
                .extracting("reason")
                .isEqualTo(AgreementException.Reason.PETITION_NOT_AGREEABLE);
    }

    private Petition petition(User writer, int target) {
        return Petition.create(
                writer, PetitionCategory.FACILITY,
                "title", "content", target, now.minusDays(1));
    }

    private User user(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }
}