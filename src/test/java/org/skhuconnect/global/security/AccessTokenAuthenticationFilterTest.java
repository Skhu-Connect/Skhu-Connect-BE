package org.skhuconnect.global.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccessTokenAuthenticationFilterTest {

    private org.skhuconnect.user.repository.UserRepository users;
    private JwtDecoder jwtDecoder;
    private AccessTokenAuthenticationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtDecoder = mock(JwtDecoder.class);
        users = mock(org.skhuconnect.user.repository.UserRepository.class);
        when(users.existsByIdAndDeletedFalse(org.mockito.ArgumentMatchers.anyLong())).thenReturn(true);
        filter = new AccessTokenAuthenticationFilter(jwtDecoder, users);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void validUserTokenInjectsUserIdIntoPetitionRequest() throws Exception {
        MockHttpServletRequest request = petitionRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtDecoder.decode("valid-token")).thenReturn(userJwt("42", "USER"));

        filter.doFilter(request, response, filterChain);

        assertThat(request.getAttribute(
                AccessTokenAuthenticationFilter.USER_ID_ATTRIBUTE)).isEqualTo(42L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void missingBearerTokenReturnsUnauthorized() throws Exception {
        MockHttpServletRequest request = petitionRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Unauthorized");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception {
        MockHttpServletRequest request = petitionRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtDecoder.decode("invalid-token")).thenThrow(new JwtValidationException(
                "invalid token", List.of(new OAuth2Error("invalid_token"))));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void expiredTokenReturnsUnauthorized() throws Exception {
        MockHttpServletRequest request = petitionRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtDecoder.decode("expired-token")).thenThrow(
                new JwtValidationException(
                        "token expired",
                        List.of(new OAuth2Error("invalid_token"))));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType())
                .isEqualTo("application/problem+json");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void nonUserRoleReturnsUnauthorized() throws Exception {
        MockHttpServletRequest request = petitionRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer admin-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtDecoder.decode("admin-token")).thenReturn(userJwt("42", "ADMIN"));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void nonNumericSubjectReturnsUnauthorized() throws Exception {
        MockHttpServletRequest request = petitionRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-subject");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtDecoder.decode("invalid-subject")).thenReturn(userJwt("user", "USER"));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void optionsPreflightDoesNotRequireToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "OPTIONS", "/connect/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtDecoder, never()).decode(org.mockito.ArgumentMatchers.anyString());
    }
    @Test
    void nonPetitionRequestDoesNotRequireToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/connect/departments");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtDecoder, never()).decode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void petitionListGetDoesNotRequireToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/connect/petitions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtDecoder, never()).decode(
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void petitionDetailGetDoesNotRequireToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/connect/petitions/10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtDecoder, never()).decode(
                org.mockito.ArgumentMatchers.anyString());
    }
    @Test
    void petitionWriteMethodsRequireToken() throws Exception {
        for (String method : List.of("POST", "PUT", "DELETE")) {
            MockHttpServletRequest request = new MockHttpServletRequest(
                    method, method.equals("POST")
                            ? "/connect/petitions"
                            : "/connect/petitions/10");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
        }
    }

    @Test
    void publicGetIgnoresValidOrInvalidAuthorizationHeader() throws Exception {
        for (String token : List.of("valid-token", "invalid-token")) {
            MockHttpServletRequest request = new MockHttpServletRequest(
                    "GET", "/connect/petitions/10");
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(jwtDecoder, never()).decode(token);
        }
    }

    @Test
    void arbitraryPetitionSubPathIsNotPublic() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/connect/petitions/10/unknown");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
    }
    @Test
    void agreementWriteRequestRequiresToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/connect/petitions/10/agreements");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }
    @Test
    void notificationApisRequireToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/connect/notifications");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void userActivityApisRequireToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/connect/users/me/petitions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void validUserTokenInjectsUserIdIntoUserActivityRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/connect/users/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtDecoder.decode("valid-token")).thenReturn(userJwt("42", "USER"));

        filter.doFilter(request, response, filterChain);

        assertThat(request.getAttribute(
                AccessTokenAuthenticationFilter.USER_ID_ATTRIBUTE)).isEqualTo(42L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void adminTokenIsAcceptedOnlyForAdminProtectedRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/connect/admin/notification-logs");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer admin-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtDecoder.decode("admin-token")).thenReturn(userJwt("7", "ADMIN"));

        filter.doFilter(request, response, filterChain);

        assertThat(request.getAttribute(
                AccessTokenAuthenticationFilter.ADMIN_ID_ATTRIBUTE)).isEqualTo(7L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void userTokenCannotAccessAdminProtectedRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/connect/admin/notification-logs");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer user-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtDecoder.decode("user-token")).thenReturn(userJwt("42", "USER"));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }
    @Test
    void withdrawnUserAccessTokenIsRejectedImmediately() throws Exception {
        MockHttpServletRequest request = petitionRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer withdrawn-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtDecoder.decode("withdrawn-token")).thenReturn(userJwt("42", "USER"));
        when(users.existsByIdAndDeletedFalse(42L)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    private MockHttpServletRequest petitionRequest() {
        return new MockHttpServletRequest("POST", "/connect/petitions");
    }

    private Jwt userJwt(String subject, String role) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(subject)
                .claim("role", role)
                .build();
    }
}
