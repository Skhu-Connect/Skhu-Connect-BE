package org.skhuconnect.admin.token.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.admin.token.exception.AdminAuthException;
import org.skhuconnect.admin.token.exception.AdminAuthExceptionHandler;
import org.skhuconnect.admin.token.service.AdminAuthService;
import org.skhuconnect.admin.token.service.AdminRefreshTokenCookieService;
import org.skhuconnect.auth.token.config.JwtProperties;
import org.skhuconnect.auth.token.dto.TokenIssueResult;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAuthControllerTest {

    private AdminAuthService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AdminAuthService.class);
        AdminRefreshTokenCookieService cookies = new AdminRefreshTokenCookieService(
                new JwtProperties("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=", false, "Lax"));
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminAuthController(service, cookies))
                .setControllerAdvice(new AdminAuthExceptionHandler())
                .build();
    }

    @Test
    void loginReturnsSeparateHttpOnlyAdminRefreshCookie() throws Exception {
        when(service.login("operator", "password"))
                .thenReturn(new TokenIssueResult("admin-access", 1800, "admin-refresh"));

        mockMvc.perform(post("/connect/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"operator\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("admin-access"))
                .andExpect(cookie().httpOnly("adminRefreshToken", true))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("Path=/connect/admin/auth")));
    }

    @Test
    void invalidCredentialsReturnUnauthorized() throws Exception {
        when(service.login(any(), any())).thenThrow(new AdminAuthException(
                AdminAuthException.Reason.INVALID_CREDENTIALS));
        mockMvc.perform(post("/connect/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"operator\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid credentials"));
    }
}