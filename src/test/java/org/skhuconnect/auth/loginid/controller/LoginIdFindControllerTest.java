package org.skhuconnect.auth.loginid.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.auth.email.exception.EmailVerificationException;
import org.skhuconnect.auth.loginid.dto.LoginIdResponse;
import org.skhuconnect.auth.loginid.service.LoginIdFindService;
import org.skhuconnect.auth.token.exception.UserAuthException;
import org.skhuconnect.auth.token.exception.UserAuthExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class LoginIdFindControllerTest {

    private LoginIdFindService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(LoginIdFindService.class);
        mockMvc = standaloneSetup(new LoginIdFindController(service))
                .setControllerAdvice(new UserAuthExceptionHandler())
                .build();
    }

    @Test
    void emailVerificationReturnsFullLoginId() throws Exception {
        when(service.findByEmailVerification(any()))
                .thenReturn(new LoginIdResponse("202214139"));

        mockMvc.perform(post("/connect/auth/login-id/find/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verificationToken\":\"raw-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("202214139"));
    }

    @Test
    void emailAndPasswordReturnFullLoginId() throws Exception {
        when(service.findByPassword(any()))
                .thenReturn(new LoginIdResponse("202214139"));

        mockMvc.perform(post("/connect/auth/login-id/find/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"20260000@office.skhu.ac.kr",
                                 "password":"current-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("202214139"));
    }

    @Test
    void missingEmailAndWrongPasswordReturnSameUnauthorizedResponse() throws Exception {
        when(service.findByPassword(any())).thenThrow(new UserAuthException(
                UserAuthException.Reason.INVALID_CREDENTIALS));

        mockMvc.perform(post("/connect/auth/login-id/find/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"missing@office.skhu.ac.kr",
                                 "password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid credentials"));
    }

    @Test
    void usedAndExpiredVerificationTokensKeepExistingStatusPolicy() throws Exception {
        when(service.findByEmailVerification(any()))
                .thenThrow(new EmailVerificationException(
                        EmailVerificationException.Reason.TOKEN_USED))
                .thenThrow(new EmailVerificationException(
                        EmailVerificationException.Reason.TOKEN_EXPIRED));

        var request = post("/connect/auth/login-id/find/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"verificationToken\":\"raw-token\"}");
        mockMvc.perform(request).andExpect(status().isConflict());
        mockMvc.perform(request).andExpect(status().isGone());
    }

    @Test
    void blankRequestsAreBadRequest() throws Exception {
        mockMvc.perform(post("/connect/auth/login-id/find/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verificationToken\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/connect/auth/login-id/find/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
