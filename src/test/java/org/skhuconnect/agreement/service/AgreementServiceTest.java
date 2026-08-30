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
import static org.mockito.Mockito.never;
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
    void writerCannotAgreeOwnPetition() {
        // 자기 글 공감을 막는다 - 도달률을 스스로 올릴 수 있었고, "공감 0건이어야 삭제 가능" 규칙도
        // 본인 공감이 섞이면 뜻이 어긋난다(작성자가 자기 글에 공감만 눌러도 영영 못 지우게 됐다).
        User writer = user(1L);
        Petition petition = petition(writer, 10);
        ReflectionTestUtils.setField(petition, "id", 10L);
        when(petitionRepository.findVisibleByIdForUpdate(10L))
                .thenReturn(Optional.of(petition));
        when(userRepository.findById(1L)).thenReturn(Optional.of(writer));

        assertThatThrownBy(() -> service.agree(1L, 10L))
                .isInstanceOf(AgreementException.class)
                .extracting("reason")
                .isEqualTo(AgreementException.Reason.SELF_AGREEMENT_NOT_ALLOWED);
        verify(agreementRepository, never()).saveAndFlush(any());
        assertThat(petition.getAgreementCount()).isZero();
    }

    @Test
    void nonOpenPetitionReportsNotAgreeableEvenForSelfAgreement() {
        // 상태 자체가 안 되는 청원은 자기 글이어도 "공감 불가" 사유가 먼저 나가야 한다 -
        // assertNotAgreeable 이 검증하는 세 경우(만료·비공개·숨김) 모두 작성자 본인이 시도한다.
        Petition nonOpen = petition(user(1L), 10);
        ReflectionTestUtils.setField(nonOpen, "status", PetitionStatus.UNDER_REVIEW);
        assertNotAgreeable(nonOpen);
    }

    @Test
    void writerCanCancelAlreadyExistingSelfAgreement() {
        // 이 규칙을 넣기 전에 자기 글에 공감해 둔 사용자를 위한 탈출구 - cancel() 은 그대로 둬서
        // 기존 자기 공감을 스스로 취소하고(그러면 0건이 되어) 삭제할 수 있게 한다.
        Petition petition = petition(user(1L), 10);
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