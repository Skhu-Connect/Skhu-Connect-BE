package org.skhuconnect.petition.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skhuconnect.petition.dto.request.PetitionCreateRequest;
import org.skhuconnect.petition.dto.request.PetitionQueryCondition;
import org.skhuconnect.petition.dto.request.PetitionUpdateRequest;
import org.skhuconnect.admin.answer.repository.OfficialAnswerRepository;
import org.skhuconnect.petition.dto.response.PetitionPageResponse;
import org.skhuconnect.petition.dto.response.PetitionQueryResponse;
import org.skhuconnect.petition.dto.response.PetitionResponse;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.entity.PetitionStatus;
import org.skhuconnect.petition.exception.PetitionException;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.threshold.entity.ThresholdSetting;
import org.skhuconnect.threshold.repository.ThresholdSettingRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PetitionServiceTest {

    private PetitionRepository petitionRepository;
    private UserRepository userRepository;
    private ThresholdSettingRepository thresholdSettingRepository;
    private OfficialAnswerRepository officialAnswerRepository;
    private Clock clock;
    private PetitionService service;

    @BeforeEach
    void setUp() {
        petitionRepository = mock(PetitionRepository.class);
        userRepository = mock(UserRepository.class);
        thresholdSettingRepository = mock(ThresholdSettingRepository.class);
        officialAnswerRepository = mock(OfficialAnswerRepository.class);
        clock = Clock.fixed(Instant.parse("2026-08-05T03:00:00Z"),
                ZoneId.of("Asia/Seoul"));
        service = new PetitionService(
                petitionRepository, userRepository, thresholdSettingRepository, officialAnswerRepository, clock);
    }

    @Test
    void createCalculatesAndStoresTargetAgreementCount() {
        User writer = mockUser(1L);
        ThresholdSetting setting = ThresholdSetting.create(
                PetitionCategory.FACILITY, 1234, new BigDecimal("0.0100"), 5);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(writer));
        when(thresholdSettingRepository.findByCategory(PetitionCategory.FACILITY))
                .thenReturn(Optional.of(setting));
        when(petitionRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PetitionResponse response = service.create(1L, new PetitionCreateRequest(
                PetitionCategory.FACILITY, "시설 개선", "시설을 개선해주세요."));

        ArgumentCaptor<Petition> captor = ArgumentCaptor.forClass(Petition.class);
        verify(petitionRepository).save(captor.capture());
        Petition saved = captor.getValue();
        assertThat(saved.getWriter()).isSameAs(writer);
        assertThat(saved.getTargetAgreementCount()).isEqualTo(13);
        assertThat(saved.getAgreementDeadline())
                .isEqualTo(LocalDateTime.of(2026, 8, 5, 12, 0).plusDays(30));
        assertThat(response.title()).isEqualTo("시설 개선");
    }

    @Test
    void createRejectsUntilImmediatelyBeforeCooldownBoundary() {
        User writer = mockUser(1L);
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 12, 0);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(writer));
        when(petitionRepository.findLatestCreatedAtByWriterId(1L))
                .thenReturn(Optional.of(now.minusMinutes(10).plusNanos(1)));

        assertThatThrownBy(() -> service.create(1L, createRequest()))
                .isInstanceOf(PetitionException.class)
                .extracting("reason")
                .isEqualTo(PetitionException.Reason.PETITION_CREATE_COOLDOWN);
        org.mockito.Mockito.verify(petitionRepository, org.mockito.Mockito.never())
                .save(any());
        org.mockito.Mockito.verify(thresholdSettingRepository, org.mockito.Mockito.never())
                .findByCategory(any());
    }

    @Test
    void createAllowsAtExactCooldownBoundary() {
        User writer = mockUser(1L);
        ThresholdSetting setting = ThresholdSetting.create(
                PetitionCategory.FACILITY, 1234, new BigDecimal("0.0100"), 5);
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 12, 0);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(writer));
        when(petitionRepository.findLatestCreatedAtByWriterId(1L))
                .thenReturn(Optional.of(now.minusMinutes(10)));
        when(thresholdSettingRepository.findByCategory(PetitionCategory.FACILITY))
                .thenReturn(Optional.of(setting));
        when(petitionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PetitionResponse response = service.create(1L, createRequest());

        assertThat(response.title()).isEqualTo("시설 개선");
        verify(petitionRepository).save(any());
    }

    @Test
    void createRejectsUnknownUser() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(1L, createRequest()))
                .isInstanceOf(PetitionException.class)
                .extracting("reason")
                .isEqualTo(PetitionException.Reason.USER_NOT_FOUND);
    }

    @Test
    void createRejectsMissingThresholdSetting() {
        User writer = mockUser(1L);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(writer));
        when(thresholdSettingRepository.findByCategory(PetitionCategory.FACILITY))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(1L, createRequest()))
                .isInstanceOf(PetitionException.class)
                .extracting("reason")
                .isEqualTo(PetitionException.Reason.THRESHOLD_SETTING_NOT_FOUND);
    }

    @Test
    void updateChangesTitleAndContentForWriter() {
        Petition petition = petition(mockUser(1L));
        when(petitionRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(petition));

        PetitionResponse response = service.update(
                1L, 10L, new PetitionUpdateRequest("수정 제목", "수정 내용"));

        assertThat(response.title()).isEqualTo("수정 제목");
        assertThat(response.content()).isEqualTo("수정 내용");
    }

    @Test
    void updateRejectsDifferentUser() {
        Petition petition = petition(mockUser(2L));
        when(petitionRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(petition));

        assertThatThrownBy(() -> service.update(
                1L, 10L, new PetitionUpdateRequest("수정 제목", "수정 내용")))
                .isInstanceOf(PetitionException.class)
                .extracting("reason")
                .isEqualTo(PetitionException.Reason.PETITION_FORBIDDEN);
    }

    @Test
    void deleteSoftDeletesPetition() {
        Petition petition = petition(mockUser(1L));
        when(petitionRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(petition));

        service.delete(1L, 10L);

        assertThat(petition.isDeleted()).isTrue();
        assertThat(petition.getDeletedAt())
                .isEqualTo(LocalDateTime.of(2026, 8, 5, 12, 0));
    }

    @Test
    void missingOrDeletedPetitionIsNotFound() {
        when(petitionRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 10L))
                .isInstanceOf(PetitionException.class)
                .extracting("reason")
                .isEqualTo(PetitionException.Reason.PETITION_NOT_FOUND);
    }

    @Test
    void agreementCountPreventsUpdateAndDelete() {
        Petition petition = petition(mockUser(1L));
        ReflectionTestUtils.setField(petition, "agreementCount", 1);

        assertUpdateAndDeleteNotEditable(petition);
    }

    @Test
    void nonOpenStatusPreventsUpdateAndDelete() {
        for (PetitionStatus status : new PetitionStatus[]{
                PetitionStatus.UNDER_REVIEW,
                PetitionStatus.ANSWERED,
                PetitionStatus.EXPIRED
        }) {
            Petition petition = petition(mockUser(1L));
            ReflectionTestUtils.setField(petition, "status", status);

            assertUpdateAndDeleteNotEditable(petition);
        }
    }

    @Test
    void hiddenPetitionPreventsUpdateAndDelete() {
        Petition petition = petition(mockUser(1L));
        ReflectionTestUtils.setField(petition, "hidden", true);

        assertUpdateAndDeleteNotEditable(petition);
    }

    @Test
    void deletedPetitionPreventsUpdateAndDelete() {
        Petition petition = petition(mockUser(1L));
        ReflectionTestUtils.setField(petition, "deleted", true);

        assertUpdateAndDeleteNotEditable(petition);
    }

    @Test
    void softDeletedPetitionCannotBeUpdatedOrDeletedAgain() {
        Petition petition = petition(mockUser(1L));
        when(petitionRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(petition));
        service.delete(1L, 10L);

        assertUpdateNotEditable();
        assertDeleteNotEditable();
    }
    @Test
    void queryDisplaysExpiredEffectiveStatus() {
        Petition expired = petition(mockUser(1L));
        ReflectionTestUtils.setField(
                expired, "agreementDeadline", LocalDateTime.of(2026, 8, 5, 11, 59));
        when(petitionRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(expired)));

        PetitionPageResponse response = service.findAll(
                new PetitionQueryCondition(null, null, null));

        assertThat(response.content()).extracting(PetitionQueryResponse::status)
                .containsExactly(PetitionStatus.EXPIRED);
    }

    @Test
    void queryMapsExpiresAtSortToAgreementDeadline() {
        when(petitionRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        service.findAll(new PetitionQueryCondition(
                null, null, null, 0, 20, "expiresAt,asc"));

        verify(petitionRepository).findAll(
                any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("agreementDeadline"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void queryRejectsUnsupportedSortProperty() {
        assertThatThrownBy(() -> service.findAll(new PetitionQueryCondition(
                null, null, null, 0, 20, "title,desc")))
                .isInstanceOf(PetitionException.class)
                .extracting("reason")
                .isEqualTo(PetitionException.Reason.INVALID_SORT);
    }

    @Test
    void detailIncludesOfficialAnswerWithoutExposingAdministratorIdentity() {
        Petition petition = petition(mockUser(1L));
        org.skhuconnect.admin.answer.entity.OfficialAnswer answer = mock(org.skhuconnect.admin.answer.entity.OfficialAnswer.class);
        when(answer.getContent()).thenReturn("official answer");
        when(answer.getAnswerSource()).thenReturn(org.skhuconnect.admin.answer.entity.AnswerSource.SCHOOL_OFFICIAL);
        when(answer.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 8, 5, 12, 0));
        when(answer.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 8, 5, 12, 0));
        when(petitionRepository.findByIdAndDeletedFalseAndHiddenFalse(10L)).thenReturn(Optional.of(petition));
        when(officialAnswerRepository.findByPetitionId(10L)).thenReturn(Optional.of(answer));

        PetitionQueryResponse response = service.findDetail(10L);

        assertThat(response.officialAnswer().content()).isEqualTo("official answer");
        assertThat(response.officialAnswer().answerSource()).isEqualTo(org.skhuconnect.admin.answer.entity.AnswerSource.SCHOOL_OFFICIAL);
    }

    @Test
    void detailRejectsHiddenDeletedOrMissingPetition() {
        when(petitionRepository.findByIdAndDeletedFalseAndHiddenFalse(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findDetail(10L))
                .isInstanceOf(PetitionException.class)
                .extracting("reason")
                .isEqualTo(PetitionException.Reason.PETITION_NOT_FOUND);
    }
    @Test
    void methodsHaveTransactionBoundaries() throws Exception {
        assertThat(PetitionService.class.getMethod(
                "create", Long.class, PetitionCreateRequest.class)
                .getAnnotation(Transactional.class)).isNotNull();
        assertThat(PetitionService.class.getMethod(
                "update", Long.class, Long.class, PetitionUpdateRequest.class)
                .getAnnotation(Transactional.class)).isNotNull();
        assertThat(PetitionService.class.getMethod(
                "delete", Long.class, Long.class)
                .getAnnotation(Transactional.class)).isNotNull();
    }

    private void assertUpdateAndDeleteNotEditable(Petition petition) {
        when(petitionRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(petition));

        assertUpdateNotEditable();
        assertDeleteNotEditable();
    }

    private void assertUpdateNotEditable() {
        assertThatThrownBy(() -> service.update(
                1L, 10L, new PetitionUpdateRequest("수정 제목", "수정 내용")))
                .isInstanceOf(PetitionException.class)
                .extracting("reason")
                .isEqualTo(PetitionException.Reason.PETITION_NOT_EDITABLE);
    }

    private void assertDeleteNotEditable() {
        assertThatThrownBy(() -> service.delete(1L, 10L))
                .isInstanceOf(PetitionException.class)
                .extracting("reason")
                .isEqualTo(PetitionException.Reason.PETITION_NOT_EDITABLE);
    }
    private PetitionCreateRequest createRequest() {
        return new PetitionCreateRequest(
                PetitionCategory.FACILITY, "시설 개선", "시설을 개선해주세요.");
    }

    private Petition petition(User writer) {
        return Petition.create(
                writer,
                PetitionCategory.FACILITY,
                "기존 제목",
                "기존 내용",
                13,
                LocalDateTime.of(2026, 8, 5, 12, 0)
        );
    }

    private User mockUser(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }
}
