package org.skhuconnect.auth.email.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.skhuconnect.auth.email.dto.response.EmailVerificationConfirmResponse;
import org.skhuconnect.auth.email.entity.EmailVerificationPurpose;
import org.skhuconnect.auth.email.exception.EmailVerificationExceptionHandler;
import org.skhuconnect.auth.email.service.EmailVerificationService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmailVerificationControllerTest {
    private EmailVerificationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(EmailVerificationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EmailVerificationController(service))
                .setControllerAdvice(new EmailVerificationExceptionHandler())
                .build();
    }

    @Test
    void sendReturnsNoContentWithoutSensitiveBody() throws Exception {
        mockMvc.perform(post("/connect/auth/email-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student@office.skhu.ac.kr",
                                  "purpose": "SIGN_UP"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void invalidSendRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/connect/auth/email-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "purpose": null
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmReturnsOnlyRawTokenAndExpiry() throws Exception {
        when(service.confirm("student@office.skhu.ac.kr", "123456",
                EmailVerificationPurpose.SIGN_UP))
                .thenReturn(new EmailVerificationConfirmResponse("one-time-token", 1800));

        mockMvc.perform(post("/connect/auth/email-verifications/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student@office.skhu.ac.kr",
                                  "code": "123456",
                                  "purpose": "SIGN_UP"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationToken").value("one-time-token"))
                .andExpect(jsonPath("$.expiresInSeconds").value(1800))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.codeHash").doesNotExist())
                .andExpect(jsonPath("$.tokenHash").doesNotExist());
    }

    @Test
    void invalidCodeFormatReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/connect/auth/email-verifications/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student@office.skhu.ac.kr",
                                  "code": "12345",
                                  "purpose": "SIGN_UP"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exposesSwaggerTagAndOperations() throws Exception {
        assertThat(EmailVerificationController.class).hasAnnotation(Tag.class);
        assertThat(EmailVerificationController.class.getMethod(
                "send", org.skhuconnect.auth.email.dto.request
                        .EmailVerificationSendRequest.class)
                .getAnnotation(Operation.class)).isNotNull();
        assertThat(EmailVerificationController.class.getMethod(
                "confirm", org.skhuconnect.auth.email.dto.request
                        .EmailVerificationConfirmRequest.class)
                .getAnnotation(Operation.class)).isNotNull();
    }
}
