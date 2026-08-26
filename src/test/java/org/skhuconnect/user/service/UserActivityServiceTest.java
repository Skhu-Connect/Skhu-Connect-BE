package org.skhuconnect.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skhuconnect.agreement.repository.AgreementRepository;
import org.skhuconnect.bookmark.repository.BookmarkRepository;
import org.skhuconnect.comment.repository.CommentLikeRepository;
import org.skhuconnect.comment.repository.CommentRepository;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.notification.dto.NotificationPageResponse;
import org.skhuconnect.notification.service.NotificationService;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.dto.NotificationSettingsUpdateRequest;
import org.skhuconnect.user.exception.UserActivityException;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserActivityServiceTest {

    private UserRepository users;
    private PetitionRepository petitions;
    private AgreementRepository agreements;
    private BookmarkRepository bookmarks;
    private CommentRepository comments;
    private CommentLikeRepository commentLikes;
    private NotificationService notifications;
    private UserActivityService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        petitions = mock(PetitionRepository.class);
        agreements = mock(AgreementRepository.class);
        bookmarks = mock(BookmarkRepository.class);
        comments = mock(CommentRepository.class);
        commentLikes = mock(CommentLikeRepository.class);
        notifications = mock(NotificationService.class);
        service = new UserActivityService(
                users, petitions, agreements, bookmarks, comments,
                commentLikes, notifications,
                Clock.fixed(Instant.parse("2026-08-06T03:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void returnsOwnProfileWithoutInternalIdOrPassword() {
        Department department = mock(Department.class);
        when(department.getCode()).thenReturn("SW");
        when(department.getName()).thenReturn("소프트웨어공학과");
        User user = User.create(
                "user@office.skhu.ac.kr", "login-user", "encoded", department);
        when(users.findById(1L)).thenReturn(Optional.of(user));

        var response = service.findMe(1L);

        assertThat(response.email()).isEqualTo("user@office.skhu.ac.kr");
        assertThat(response.loginId()).isEqualTo("login-user");
        assertThat(response.departmentCode()).isEqualTo("SW");
        assertThat(response.notificationEnabled()).isTrue();
        assertThat(response.notificationSettings().like()).isTrue();
    }

    @Test
    void updatesOnlyProvidedNotificationSettings() {
        User user = User.create(
                "user@office.skhu.ac.kr", "login-user", "encoded",
                mock(Department.class));
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(users.findById(1L)).thenReturn(Optional.of(user));

        var response = service.updateNotificationSettings(1L,
                new NotificationSettingsUpdateRequest(
                        null, null, null, false, null));

        assertThat(response.agreement()).isTrue();
        assertThat(response.answer()).isTrue();
        assertThat(response.reply()).isTrue();
        assertThat(response.like()).isFalse();
        assertThat(response.notice()).isTrue();
        assertThat(service.findMe(1L).notificationSettings().like()).isFalse();
    }

    @Test
    void rejectsEmptyNotificationSettings() {
        assertThatThrownBy(() -> service.updateNotificationSettings(1L,
                new NotificationSettingsUpdateRequest(
                        null, null, null, null, null)))
                .isInstanceOf(UserActivityException.class)
                .extracting("reason")
                .isEqualTo(UserActivityException.Reason.INVALID_NOTIFICATION_SETTINGS);
    }

    @Test
    void missingUserCannotUpdateNotificationSettings() {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateNotificationSettings(1L,
                new NotificationSettingsUpdateRequest(
                        null, true, null, null, null)))
                .isInstanceOf(UserActivityException.class)
                .extracting("reason")
                .isEqualTo(UserActivityException.Reason.USER_NOT_FOUND);
    }

    @Test
    void missingUserIsNotFound() {
        when(users.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findMe(1L))
                .isInstanceOf(UserActivityException.class)
                .extracting("reason")
                .isEqualTo(UserActivityException.Reason.USER_NOT_FOUND);
    }

    @Test
    void activityQueriesUseAuthenticatedUserAndStableNewestSort() {
        when(petitions.findVisibleByWriterId(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(agreements.findVisibleByUserId(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(bookmarks.findVisibleByUserId(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(comments.findVisibleActivityByWriterId(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.findMyPetitions(7L, 0, 20);
        service.findMyAgreements(7L, 0, 20);
        service.findMyBookmarks(7L, 0, 20);
        service.findMyComments(7L, 0, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(petitions).findVisibleByWriterId(
                org.mockito.ArgumentMatchers.eq(7L), pageable.capture());
        assertThat(pageable.getValue().getSort().toString())
                .isEqualTo("createdAt: DESC,id: DESC");
        verify(agreements).findVisibleByUserId(
                org.mockito.ArgumentMatchers.eq(7L), any());
        verify(bookmarks).findVisibleByUserId(
                org.mockito.ArgumentMatchers.eq(7L), any());
        verify(comments).findVisibleActivityByWriterId(
                org.mockito.ArgumentMatchers.eq(7L), any());
    }

    @Test
    void notificationsReuseExistingReceiverScopedQuery() {
        NotificationPageResponse response = new NotificationPageResponse(
                List.of(), 0, 20, 0, 0, true, true);
        when(notifications.findAll(9L, 0, 20)).thenReturn(response);

        assertThat(service.findMyNotifications(9L, 0, 20)).isSameAs(response);
        verify(notifications).findAll(9L, 0, 20);
    }

    @Test
    void rejectsInvalidPage() {
        assertThatThrownBy(() -> service.findMyPetitions(1L, -1, 20))
                .isInstanceOf(UserActivityException.class)
                .extracting("reason")
                .isEqualTo(UserActivityException.Reason.INVALID_PAGE);
        assertThatThrownBy(() -> service.findMyComments(1L, 0, 101))
                .isInstanceOf(UserActivityException.class);
    }
}
