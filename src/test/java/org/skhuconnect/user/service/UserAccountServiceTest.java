package org.skhuconnect.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.user.dto.LoginIdUpdateRequest;
import org.skhuconnect.user.dto.PasswordChangeRequest;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.exception.UserActivityException;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountServiceTest {

    private UserRepository users;
    private PasswordEncoder passwords;
    private UserAccountService service;
    private User user;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        passwords = mock(PasswordEncoder.class);
        service = new UserAccountService(users, passwords);
        user = User.create("student@office.skhu.ac.kr", "student01",
                "encoded-current", Department.create("CS", "컴퓨터공학과"));
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
    }

    @Test
    void changesTrimmedLoginIdAfterPasswordVerification() {
        when(passwords.matches("current-password", "encoded-current"))
                .thenReturn(true);

        var response = service.changeLoginId(1L,
                new LoginIdUpdateRequest("  new-login-id  ", "current-password"));

        assertThat(response.loginId()).isEqualTo("new-login-id");
        assertThat(user.getLoginId()).isEqualTo("new-login-id");
        verify(users).findByIdForUpdate(1L);
        verify(users).flush();
    }

    @Test
    void duplicateLoginIdAndUniqueConstraintViolationAreConflict() {
        when(passwords.matches("current-password", "encoded-current"))
                .thenReturn(true);
        when(users.existsByLoginId("duplicate")).thenReturn(true);
        assertReason(() -> service.changeLoginId(1L,
                        new LoginIdUpdateRequest("duplicate", "current-password")),
                UserActivityException.Reason.LOGIN_ID_ALREADY_EXISTS);

        when(users.existsByLoginId("racing-id")).thenReturn(false);
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(users).flush();
        assertReason(() -> service.changeLoginId(1L,
                        new LoginIdUpdateRequest("racing-id", "current-password")),
                UserActivityException.Reason.LOGIN_ID_ALREADY_EXISTS);
    }

    @Test
    void wrongPasswordDoesNotChangeLoginId() {
        when(passwords.matches("wrong", "encoded-current")).thenReturn(false);

        assertReason(() -> service.changeLoginId(1L,
                        new LoginIdUpdateRequest("new-login-id", "wrong")),
                UserActivityException.Reason.CURRENT_PASSWORD_MISMATCH);

        assertThat(user.getLoginId()).isEqualTo("student01");
        verify(users, never()).flush();
    }

    @Test
    void currentLoginIdIsRejected() {
        when(passwords.matches("current-password", "encoded-current"))
                .thenReturn(true);

        assertReason(() -> service.changeLoginId(1L,
                        new LoginIdUpdateRequest("student01", "current-password")),
                UserActivityException.Reason.LOGIN_ID_UNCHANGED);
    }

    @Test
    void changesPasswordAfterCurrentPasswordVerification() {
        when(passwords.matches("current-password", "encoded-current"))
                .thenReturn(true);
        when(passwords.matches("newPassword1", "encoded-current"))
                .thenReturn(false);
        when(passwords.encode("newPassword1")).thenReturn("encoded-new");

        service.changePassword(1L,
                new PasswordChangeRequest("current-password", "newPassword1"));

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        verify(users).findByIdForUpdate(1L);
    }

    @Test
    void wrongPasswordDoesNotChangePassword() {
        when(passwords.matches("wrong", "encoded-current")).thenReturn(false);

        assertReason(() -> service.changePassword(1L,
                        new PasswordChangeRequest("wrong", "newPassword1")),
                UserActivityException.Reason.CURRENT_PASSWORD_MISMATCH);

        assertThat(user.getPassword()).isEqualTo("encoded-current");
        verify(passwords, never()).encode("newPassword1");
    }

    @Test
    void currentPasswordCannotBeReusedAsNewPassword() {
        when(passwords.matches("currentPassword1", "encoded-current"))
                .thenReturn(true);

        assertReason(() -> service.changePassword(1L,
                        new PasswordChangeRequest(
                                "currentPassword1", "currentPassword1")),
                UserActivityException.Reason.PASSWORD_UNCHANGED);
        assertThat(user.getPassword()).isEqualTo("encoded-current");
    }

    @Test
    void missingUserIsNotFound() {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertReason(() -> service.changePassword(1L,
                        new PasswordChangeRequest(
                                "current-password", "newPassword1")),
                UserActivityException.Reason.USER_NOT_FOUND);
    }

    @Test
    void invalidLoginIdPolicyIsRejectedBeforeDuplicateCheck() {
        when(passwords.matches("current-password", "encoded-current"))
                .thenReturn(true);

        assertReason(() -> service.changeLoginId(1L,
                        new LoginIdUpdateRequest("학생12345", "current-password")),
                UserActivityException.Reason.INVALID_ACCOUNT_REQUEST);

        verify(users, never()).existsByLoginId("학생12345");
    }

    @Test
    void invalidNewPasswordPolicyIsRejectedBeforeEncoding() {
        when(passwords.matches("current-password", "encoded-current"))
                .thenReturn(true);

        assertReason(() -> service.changePassword(1L,
                        new PasswordChangeRequest("current-password", "abc-12")),
                UserActivityException.Reason.INVALID_ACCOUNT_REQUEST);

        verify(passwords, never()).encode("abc-12");
    }

    private void assertReason(Runnable action, UserActivityException.Reason reason) {
        assertThatThrownBy(action::run)
                .isInstanceOf(UserActivityException.class)
                .extracting("reason")
                .isEqualTo(reason);
    }
}
