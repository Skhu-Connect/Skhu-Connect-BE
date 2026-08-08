package org.skhuconnect.admin.answer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.admin.answer.dto.OfficialAnswerRequest;
import org.skhuconnect.admin.answer.entity.AnswerSource;
import org.skhuconnect.admin.answer.entity.OfficialAnswer;
import org.skhuconnect.admin.answer.exception.AdminOfficialAnswerException;
import org.skhuconnect.admin.answer.repository.OfficialAnswerRepository;
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.repository.AdminRepository;
import org.skhuconnect.notification.service.NotificationEventService;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.entity.PetitionStatus;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminOfficialAnswerServiceTest {

    private AdminRepository admins;
    private PetitionRepository petitions;
    private OfficialAnswerRepository answers;
    private NotificationEventService notifications;
    private AdminOfficialAnswerService service;
    private Admin admin;
    private Petition petition;

    @BeforeEach
    void setUp() {
        admins = mock(AdminRepository.class);
        petitions = mock(PetitionRepository.class);
        answers = mock(OfficialAnswerRepository.class);
        notifications = mock(NotificationEventService.class);
        service = new AdminOfficialAnswerService(admins, petitions, answers, notifications);
        admin = mock(Admin.class);
        when(admin.getId()).thenReturn(7L);
        User writer = mock(User.class);
        when(writer.getId()).thenReturn(3L);
        petition = Petition.create(writer, PetitionCategory.FACILITY, "title", "content", 1, LocalDateTime.now());
        ReflectionTestUtils.setField(petition, "id", 10L);
        petition.addAgreement(LocalDateTime.now());
    }

    @Test
    void registersFirstAnswerTransitionsPetitionAndNotifiesAudience() {
        stubAdminAndVisiblePetition();
        when(answers.findByPetitionId(10L)).thenReturn(Optional.empty());
        when(answers.saveAndFlush(any(OfficialAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.register(7L, 10L, request("official answer", AnswerSource.SCHOOL_OFFICIAL));

        assertThat(petition.getStatus()).isEqualTo(PetitionStatus.ANSWERED);
        assertThat(response.petitionId()).isEqualTo(10L);
        assertThat(response.adminId()).isEqualTo(7L);
        verify(notifications).onPetitionAnswered(petition);
    }

    @Test
    void rejectsRegistrationUnlessPetitionIsUnderReview() {
        ReflectionTestUtils.setField(petition, "status", PetitionStatus.OPEN);
        stubAdminAndVisiblePetition();

        assertThatThrownBy(() -> service.register(7L, 10L, request("answer", AnswerSource.OPERATION_TEAM)))
                .isInstanceOf(AdminOfficialAnswerException.class)
                .extracting(exception -> ((AdminOfficialAnswerException) exception).getReason())
                .isEqualTo(AdminOfficialAnswerException.Reason.PETITION_NOT_UNDER_REVIEW);

        verify(answers, never()).saveAndFlush(any());
        verify(notifications, never()).onPetitionAnswered(any());
    }

    @Test
    void rejectsSecondAnswerWithoutChangingPetitionOrNotifyingAgain() {
        stubAdminAndVisiblePetition();
        when(answers.findByPetitionId(10L)).thenReturn(Optional.of(mock(OfficialAnswer.class)));

        assertThatThrownBy(() -> service.register(7L, 10L, request("answer", AnswerSource.OPERATION_TEAM)))
                .isInstanceOf(AdminOfficialAnswerException.class)
                .extracting(exception -> ((AdminOfficialAnswerException) exception).getReason())
                .isEqualTo(AdminOfficialAnswerException.Reason.OFFICIAL_ANSWER_ALREADY_EXISTS);

        assertThat(petition.getStatus()).isEqualTo(PetitionStatus.UNDER_REVIEW);
        verify(notifications, never()).onPetitionAnswered(any());
    }

    @Test
    void updateKeepsAnsweredStateAndDoesNotSendSecondNotification() {
        ReflectionTestUtils.setField(petition, "status", PetitionStatus.ANSWERED);
        OfficialAnswer answer = OfficialAnswer.create(petition, admin, "first", AnswerSource.OPERATION_TEAM);
        Admin changedBy = mock(Admin.class);
        when(changedBy.getId()).thenReturn(8L);
        when(admins.findById(8L)).thenReturn(Optional.of(changedBy));
        when(petitions.findVisibleByIdForUpdate(10L)).thenReturn(Optional.of(petition));
        when(answers.findByPetitionId(10L)).thenReturn(Optional.of(answer));

        var response = service.update(8L, 10L, request("updated", AnswerSource.SCHOOL_OFFICIAL));

        assertThat(petition.getStatus()).isEqualTo(PetitionStatus.ANSWERED);
        assertThat(response.content()).isEqualTo("updated");
        assertThat(response.answerSource()).isEqualTo(AnswerSource.SCHOOL_OFFICIAL);
        assertThat(response.adminId()).isEqualTo(8L);
        verify(notifications, never()).onPetitionAnswered(any());
    }

    @Test
    void findsExistingAnswerWithoutChangingPetitionOrSendingNotification() {
        stubAdminAndVisiblePetition();
        OfficialAnswer answer = OfficialAnswer.create(petition, admin, "stored", AnswerSource.OPERATION_TEAM);
        when(petitions.findByIdAndDeletedFalseAndHiddenFalse(10L)).thenReturn(Optional.of(petition));
        when(answers.findByPetitionId(10L)).thenReturn(Optional.of(answer));

        var response = service.find(7L, 10L);

        assertThat(response.content()).isEqualTo("stored");
        assertThat(petition.getStatus()).isEqualTo(PetitionStatus.UNDER_REVIEW);
        verify(notifications, never()).onPetitionAnswered(any());
    }

    @Test
    void notificationFailurePropagatesFromTransactionalRegistration() throws Exception {
        stubAdminAndVisiblePetition();
        when(answers.findByPetitionId(10L)).thenReturn(Optional.empty());
        when(answers.saveAndFlush(any(OfficialAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("notification persistence failed"))
                .when(notifications).onPetitionAnswered(petition);

        assertThatThrownBy(() -> service.register(7L, 10L, request("answer", AnswerSource.OPERATION_TEAM)))
                .isInstanceOf(IllegalStateException.class);
        Method register = AdminOfficialAnswerService.class.getMethod("register", Long.class, Long.class, OfficialAnswerRequest.class);
        assertThat(register.getAnnotation(Transactional.class)).isNotNull();
    }

    private void stubAdminAndVisiblePetition() {
        when(admins.findById(7L)).thenReturn(Optional.of(admin));
        when(petitions.findVisibleByIdForUpdate(10L)).thenReturn(Optional.of(petition));
    }

    private OfficialAnswerRequest request(String content, AnswerSource source) {
        return new OfficialAnswerRequest(content, source);
    }
}