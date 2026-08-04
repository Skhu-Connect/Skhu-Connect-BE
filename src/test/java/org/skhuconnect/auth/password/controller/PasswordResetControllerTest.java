package org.skhuconnect.auth.password.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.auth.email.exception.EmailVerificationException;
import org.skhuconnect.auth.password.dto.PasswordResetRequest;
import org.skhuconnect.auth.password.exception.PasswordResetException;
import org.skhuconnect.auth.password.exception.PasswordResetExceptionHandler;
import org.skhuconnect.auth.password.service.PasswordResetService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordResetControllerTest {
    private PasswordResetService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(PasswordResetService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PasswordResetController(service))
                .setControllerAdvice(new PasswordResetExceptionHandler())
                .build();
    }

    @Test
    void resetPasswordReturnsNoContent() throws Exception {
        mockMvc.perform(validRequest())
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void invalidRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/connect/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"verificationToken":"", "newPassword":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidOrWrongPurposeTokenReturnsBadRequest() throws Exception {
        assertTokenError(EmailVerificationException.Reason.TOKEN_INVALID, 400);
        assertTokenError(EmailVerificationException.Reason.PURPOSE_MISMATCH, 400);
    }

    @Test
    void usedTokenReturnsConflict() throws Exception {
        assertTokenError(EmailVerificationException.Reason.TOKEN_USED, 409);
    }

    @Test
    void expiredTokenReturnsGone() throws Exception {
        assertTokenError(EmailVerificationException.Reason.TOKEN_EXPIRED, 410);
    }

    @Test
    void missingUserReturnsNotFound() throws Exception {
        doThrow(new PasswordResetException(PasswordResetException.Reason.USER_NOT_FOUND))
                .when(service).resetPassword(any());
        mockMvc.perform(validRequest())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User not found"));
    }

    @Test
    void swaggerDocumentsAllImplementedStatuses() throws Exception {
        assertThat(PasswordResetController.class).hasAnnotation(Tag.class);
        var method = PasswordResetController.class.getMethod(
                "resetPassword", PasswordResetRequest.class);
        assertThat(method.getAnnotation(Operation.class)).isNotNull();
        assertThat(Arrays.stream(method.getAnnotation(ApiResponses.class).value())
                .map(response -> response.responseCode()))
                .containsExactlyInAnyOrder("204", "400", "404", "409", "410");
    }

    private void assertTokenError(
            EmailVerificationException.Reason reason, int expectedStatus) throws Exception {
        doThrow(new EmailVerificationException(reason))
                .when(service).resetPassword(any());
        mockMvc.perform(validRequest())
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.title").value("Invalid verification token"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    validRequest() {
        return post("/connect/auth/password/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"verificationToken":"raw-token", "newPassword":"new-password"}
                        """);
    }
}
