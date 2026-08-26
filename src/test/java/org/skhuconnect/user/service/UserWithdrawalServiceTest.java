package org.skhuconnect.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skhuconnect.auth.email.service.EmailNormalizer;
import org.skhuconnect.auth.token.repository.RefreshTokenRepository;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.comment.entity.PetitionAnonymousNumber;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.notification.repository.FcmTokenRepository;
import org.skhuconnect.notification.entity.NotificationPoint;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.entity.UserWithdrawalHistory;
import org.skhuconnect.user.exception.UserWithdrawalException;
import org.skhuconnect.user.repository.UserRepository;
import org.skhuconnect.user.repository.UserWithdrawalHistoryRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserWithdrawalServiceTest {

    private UserRepository users;
    private UserWithdrawalHistoryRepository histories;
    private EmailNormalizer emailNormalizer;
    private RefreshTokenRepository refreshTokens;
    private FcmTokenRepository fcmTokens;
    private PasswordEncoder passwords;
    private UserEmailHasher emailHasher;
    private UserWithdrawalService service;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        users = mock(UserRepository.class);
        histories = mock(UserWithdrawalHistoryRepository.class);
        emailNormalizer = mock(EmailNormalizer.class);
        refreshTokens = mock(RefreshTokenRepository.class);
        fcmTokens = mock(FcmTokenRepository.class);
        passwords = mock(PasswordEncoder.class);
        emailHasher = mock(UserEmailHasher.class);
        Clock clock = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new UserWithdrawalService(users, histories, refreshTokens,
                fcmTokens, passwords, emailHasher, emailNormalizer, clock);
        user = User.create("student@office.skhu.ac.kr", "student",
                "bcrypt-password", Department.create("CS", "소프트웨어공학과"));
        setId(user, 7L);
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(emailNormalizer.normalize("student@office.skhu.ac.kr"))
                .thenReturn("student@office.skhu.ac.kr");
    }

    @Test
    void correctPasswordSoftDeletesAndRemovesTokensWhileKeepingContentFks() {
        LocalDateTime createdAt = LocalDateTime.of(2029, 12, 1, 0, 0);
        Petition petition = Petition.create(user, PetitionCategory.FACILITY,
                "title", "content", 100, createdAt);
        PetitionAnonymousNumber number = PetitionAnonymousNumber.create(petition, user, 1);
        Comment comment = Comment.create(petition, user, number, "comment");
        when(passwords.matches("current-password", "bcrypt-password")).thenReturn(true);
        when(emailHasher.hash("student@office.skhu.ac.kr")).thenReturn("a".repeat(64));

        service.withdraw(7L, "current-password");

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isEqualTo(LocalDateTime.of(2030, 1, 1, 0, 0));
        assertThat(user.getEmail()).isEqualTo("withdrawn-7@deleted.invalid");
        assertThat(user.getLoginId()).isEqualTo("withdrawn-7");
        assertThat(user.isNotificationEnabled()).isFalse();
        assertThat(NotificationPoint.values()).allMatch(point -> !user.allows(point));
        assertThat(petition.getWriter()).isSameAs(user);
        assertThat(comment.getWriter()).isSameAs(user);
        assertThat(petition.isDeleted()).isFalse();
        assertThat(comment.isDeleted()).isFalse();
        verify(refreshTokens).deleteByUser(user);
        verify(fcmTokens).deleteAllByUserId(7L);

        ArgumentCaptor<UserWithdrawalHistory> captor =
                ArgumentCaptor.forClass(UserWithdrawalHistory.class);
        verify(histories).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getEmailHash()).isEqualTo("a".repeat(64));
    }

    @Test
    void wrongPasswordChangesNothing() {
        when(passwords.matches("wrong", "bcrypt-password")).thenReturn(false);

        assertThatThrownBy(() -> service.withdraw(7L, "wrong"))
                .isInstanceOf(UserWithdrawalException.class)
                .extracting("reason")
                .isEqualTo(UserWithdrawalException.Reason.INVALID_PASSWORD);

        assertThat(user.isDeleted()).isFalse();
        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.getEmail()).isEqualTo("student@office.skhu.ac.kr");
        assertThat(user.getLoginId()).isEqualTo("student");
        verify(refreshTokens, never()).deleteByUser(user);
        verify(fcmTokens, never()).deleteAllByUserId(7L);
        verifyNoInteractions(histories, emailHasher);
    }

    private void setId(User target, Long id) throws Exception {
        Field field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
