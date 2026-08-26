package org.skhuconnect.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.notification.dto.NotificationPageResponse;
import org.skhuconnect.auth.loginid.dto.LoginIdResponse;
import org.skhuconnect.petition.dto.response.PetitionPageResponse;
import org.skhuconnect.user.dto.UserCommentPageResponse;
import org.skhuconnect.user.dto.UserMeResponse;
import org.skhuconnect.user.dto.NotificationSettingsResponse;
import org.skhuconnect.user.exception.UserActivityException;
import org.skhuconnect.user.exception.UserActivityExceptionHandler;
import org.skhuconnect.user.service.UserActivityService;
import org.skhuconnect.user.service.UserAccountService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class UserActivityControllerTest {

    private UserActivityService service;
    private UserAccountService accountService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(UserActivityService.class);
        accountService = mock(UserAccountService.class);
        mockMvc = standaloneSetup(new UserActivityController(service,
                        mock(org.skhuconnect.user.service.UserWithdrawalService.class),
                        accountService))
                .setControllerAdvice(new UserActivityExceptionHandler())
                .build();
    }

    @Test
    void returnsOwnProfileWithoutPasswordOrInternalUserId() throws Exception {
        when(service.findMe(1L)).thenReturn(new UserMeResponse(
                "user@office.skhu.ac.kr", "login-user",
                "SW", "소프트웨어공학과", true,
                new NotificationSettingsResponse(true, true, true, false, true)));

        mockMvc.perform(get("/connect/users/me").requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@office.skhu.ac.kr"))
                .andExpect(jsonPath("$.loginId").value("login-user"))
                .andExpect(jsonPath("$.departmentCode").value("SW"))
                .andExpect(jsonPath("$.notificationEnabled").value(true))
                .andExpect(jsonPath("$.notificationSettings.like").value(false))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void partiallyUpdatesNotificationSettingsAndReturnsAllValues() throws Exception {
        when(service.updateNotificationSettings(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(new NotificationSettingsResponse(
                        true, true, true, false, true));

        mockMvc.perform(patch("/connect/users/me/notification-settings")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"like\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agreement").value(true))
                .andExpect(jsonPath("$.answer").value(true))
                .andExpect(jsonPath("$.reply").value(true))
                .andExpect(jsonPath("$.like").value(false))
                .andExpect(jsonPath("$.notice").value(true));
    }

    @Test
    void emptyNotificationSettingsRequestIsBadRequest() throws Exception {
        when(service.updateNotificationSettings(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any()))
                .thenThrow(new UserActivityException(
                        UserActivityException.Reason.INVALID_NOTIFICATION_SETTINGS));

        mockMvc.perform(patch("/connect/users/me/notification-settings")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changesLoginIdAndTrimsRequestValue() throws Exception {
        when(accountService.changeLoginId(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(new LoginIdResponse("new-login-id"));

        mockMvc.perform(patch("/connect/users/me/login-id")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newLoginId":"  new-login-id  ",
                                 "password":"current-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("new-login-id"));

        verify(accountService).changeLoginId(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.argThat(request ->
                        request.newLoginId().equals("new-login-id")));
    }

    @Test
    void changesPasswordWithNoResponseBody() throws Exception {
        mockMvc.perform(patch("/connect/users/me/password")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"current-password",
                                 "newPassword":"new-password"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());

        verify(accountService).changePassword(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void accountChangeErrorsUseDocumentedStatuses() throws Exception {
        when(accountService.changeLoginId(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.any()))
                .thenThrow(new UserActivityException(
                        UserActivityException.Reason.LOGIN_ID_ALREADY_EXISTS));
        mockMvc.perform(patch("/connect/users/me/login-id")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newLoginId":"duplicate",
                                 "password":"current-password"}
                                """))
                .andExpect(status().isConflict());

        doThrow(new UserActivityException(
                UserActivityException.Reason.CURRENT_PASSWORD_MISMATCH))
                .when(accountService).changePassword(
                        org.mockito.ArgumentMatchers.eq(7L),
                        org.mockito.ArgumentMatchers.any());
        mockMvc.perform(patch("/connect/users/me/password")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"wrong",
                                 "newPassword":"new-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blankAccountChangeRequestsAreBadRequest() throws Exception {
        mockMvc.perform(patch("/connect/users/me/login-id")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLoginId\":\"   \",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/connect/users/me/password")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"\",\"newPassword\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exposesAllActivityEndpointsWithAuthenticatedUser() throws Exception {
        PetitionPageResponse petitions = new PetitionPageResponse(
                List.of(), 0, 20, 0, 0, true, true);
        UserCommentPageResponse comments = new UserCommentPageResponse(
                List.of(), 0, 20, 0, 0, true, true);
        NotificationPageResponse notifications = new NotificationPageResponse(
                List.of(), 0, 20, 0, 0, true, true);
        when(service.findMyPetitions(3L, 0, 20)).thenReturn(petitions);
        when(service.findMyAgreements(3L, 0, 20)).thenReturn(petitions);
        when(service.findMyBookmarks(3L, 0, 20)).thenReturn(petitions);
        when(service.findMyComments(3L, 0, 20)).thenReturn(comments);
        when(service.findMyNotifications(3L, 0, 20)).thenReturn(notifications);

        for (String path : List.of(
                "/connect/users/me/petitions",
                "/connect/users/me/agreements",
                "/connect/users/me/bookmarks",
                "/connect/users/me/comments",
                "/connect/users/me/notifications")) {
            mockMvc.perform(get(path).requestAttr("userId", 3L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        verify(service).findMyPetitions(3L, 0, 20);
        verify(service).findMyAgreements(3L, 0, 20);
        verify(service).findMyBookmarks(3L, 0, 20);
        verify(service).findMyComments(3L, 0, 20);
        verify(service).findMyNotifications(3L, 0, 20);
    }

    @Test
    void controllerHasSwaggerTag() {
        assertThat(UserActivityController.class).hasAnnotation(Tag.class);
    }
}
