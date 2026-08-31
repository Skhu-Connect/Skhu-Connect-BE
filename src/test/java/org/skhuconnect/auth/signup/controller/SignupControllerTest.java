package org.skhuconnect.auth.signup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.auth.email.exception.EmailVerificationException;
import org.skhuconnect.auth.signup.dto.SignupRequest;
import org.skhuconnect.auth.signup.exception.SignupException;
import org.skhuconnect.auth.signup.exception.SignupExceptionHandler;
import org.skhuconnect.auth.signup.service.SignupService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SignupControllerTest {

    private SignupService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(SignupService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SignupController(service))
                .setControllerAdvice(new SignupExceptionHandler())
                .build();
    }

    @Test
    void signupReturnsCreatedWithoutBody() throws Exception {
        mockMvc.perform(validRequest())
                .andExpect(status().isCreated())
                .andExpect(content().string(""));
    }

    @Test
    void termsNotAgreedReturnsBadRequest() throws Exception {
        mockMvc.perform(requestWithTerms("false", "\"1.0\""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingTermsAgreedReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/connect/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verificationToken": "raw-token",
                                  "loginId": "student01",
                                  "password": "password1",
                                  "departmentId": 1,
                                  "termsVersion": "1.0"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingTermsVersionReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/connect/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verificationToken": "raw-token",
                                  "loginId": "student01",
                                  "password": "password1",
                                  "departmentId": 1,
                                  "termsAgreed": true
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unsupportedTermsVersionReturnsBadRequest() throws Exception {
        doThrow(new SignupException(
                SignupException.Reason.UNSUPPORTED_TERMS_VERSION))
                .when(service).signup(any());

        mockMvc.perform(requestWithTerms("true", "\"2.0\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title")
                        .value("지원하지 않는 이용약관 버전입니다"));
    }

    @Test
    void existingValidationStillReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/connect/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verificationToken": "",
                                  "loginId": "",
                                  "password": "",
                                  "departmentId": 0,
                                  "termsAgreed": true,
                                  "termsVersion": "1.0"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidAccountPolicyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/connect/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verificationToken": "raw-token",
                                  "loginId": "학생12345",
                                  "password": "abc-12",
                                  "departmentId": 1,
                                  "termsAgreed": true,
                                  "termsVersion": "1.0"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateLoginIdReturnsConflictProblemDetail() throws Exception {
        doThrow(new SignupException(SignupException.Reason.LOGIN_ID_ALREADY_EXISTS))
                .when(service).signup(any());

        mockMvc.perform(validRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Login ID already exists"));
    }

    @Test
    void duplicateEmailReturnsConflictProblemDetail() throws Exception {
        doThrow(new SignupException(SignupException.Reason.EMAIL_ALREADY_EXISTS))
                .when(service).signup(any());

        mockMvc.perform(validRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email already exists"));
    }

    @Test
    void invalidTokenReturnsBadRequestProblemDetail() throws Exception {
        doThrow(new EmailVerificationException(
                EmailVerificationException.Reason.TOKEN_INVALID))
                .when(service).signup(any());

        mockMvc.perform(validRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid verification token"));
    }

    @Test
    void unknownDepartmentReturnsNotFoundProblemDetail() throws Exception {
        doThrow(new SignupException(SignupException.Reason.DEPARTMENT_NOT_FOUND))
                .when(service).signup(any());

        mockMvc.perform(validRequest())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Department not found"));
    }

    @Test
    void exposesSwaggerDocumentation() throws Exception {
        assertThat(SignupController.class).hasAnnotation(Tag.class);
        assertThat(SignupController.class.getMethod("signup", SignupRequest.class)
                .getAnnotation(Operation.class)).isNotNull();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    validRequest() {
        return requestWithTerms("true", "\"1.0\"");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    requestWithTerms(String termsAgreed, String termsVersion) {
        return post("/connect/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "verificationToken": "raw-token",
                          "loginId": "student01",
                          "password": "password1",
                          "departmentId": 1,
                          "termsAgreed": %s,
                          "termsVersion": %s
                        }
                        """.formatted(termsAgreed, termsVersion));
    }
}
