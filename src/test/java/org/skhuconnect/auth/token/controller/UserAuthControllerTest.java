package org.skhuconnect.auth.token.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.auth.token.config.JwtProperties;
import org.skhuconnect.auth.token.dto.TokenIssueResult;
import org.skhuconnect.auth.token.exception.UserAuthException;
import org.skhuconnect.auth.token.exception.UserAuthExceptionHandler;
import org.skhuconnect.auth.token.service.RefreshTokenCookieService;
import org.skhuconnect.auth.token.service.UserAuthService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAuthControllerTest {

    private UserAuthService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(UserAuthService.class);
        RefreshTokenCookieService cookies = new RefreshTokenCookieService(
                new JwtProperties("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=", false, "Lax"));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new UserAuthController(service, cookies))
                .setControllerAdvice(new UserAuthExceptionHandler())
                .build();
    }

    @Test
    void loginReturnsAccessTokenAndHttpOnlyRefreshCookie() throws Exception {
        when(service.login("student01", "password"))
                .thenReturn(new TokenIssueResult("access", 1800, "refresh"));

        mockMvc.perform(post("/connect/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"student01","password":"password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.expiresInSeconds").value(1800))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("Path=/connect/auth")))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("SameSite=Lax")));
    }

    @Test
    void invalidCredentialsReturnUnauthorizedProblemDetail() throws Exception {
        when(service.login(any(), any())).thenThrow(new UserAuthException(
                UserAuthException.Reason.INVALID_CREDENTIALS));
        mockMvc.perform(post("/connect/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"student01","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid credentials"));
    }

    @Test
    void refreshRotatesCookieAndExpiredTokenReturnsGone() throws Exception {
        when(service.refresh("old")).thenReturn(
                new TokenIssueResult("new-access", 1800, "new-refresh"));
        mockMvc.perform(post("/connect/auth/token/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "old")))
                .andExpect(status().isOk())
                .andExpect(cookie().value("refreshToken", "new-refresh"));

        when(service.refresh("expired")).thenThrow(new UserAuthException(
                UserAuthException.Reason.TOKEN_EXPIRED));
        mockMvc.perform(post("/connect/auth/token/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "expired")))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.title").value("Refresh token expired"));
    }

    @Test
    void absentRefreshTokenIsUnauthorized() throws Exception {
        when(service.refresh(null)).thenThrow(new UserAuthException(
                UserAuthException.Reason.TOKEN_INVALID));
        mockMvc.perform(post("/connect/auth/token/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid refresh token"));
    }

    @Test
    void logoutIsIdempotentAndExpiresCookie() throws Exception {
        mockMvc.perform(post("/connect/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("Max-Age=0")));
    }
}