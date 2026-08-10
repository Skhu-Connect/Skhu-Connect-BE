package org.skhuconnect.user.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.user.exception.UserActivityExceptionHandler;
import org.skhuconnect.user.exception.UserWithdrawalException;
import org.skhuconnect.user.service.UserActivityService;
import org.skhuconnect.user.service.UserWithdrawalService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class UserWithdrawalControllerTest {

    private UserWithdrawalService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(UserWithdrawalService.class);
        mockMvc = standaloneSetup(new UserActivityController(
                mock(UserActivityService.class), service))
                .setControllerAdvice(new UserActivityExceptionHandler())
                .build();
    }

    @Test
    void currentPasswordIsRequiredAndSuccessfulWithdrawalReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/connect/users/me")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"current-password\"}"))
                .andExpect(status().isNoContent());
        verify(service).withdraw(7L, "current-password");

        mockMvc.perform(delete("/connect/users/me")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).withdraw(7L, "");
    }

    @Test
    void passwordMismatchReturnsUnauthorized() throws Exception {
        doThrow(new UserWithdrawalException(
                UserWithdrawalException.Reason.INVALID_PASSWORD))
                .when(service).withdraw(7L, "wrong");

        mockMvc.perform(delete("/connect/users/me")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }
}
