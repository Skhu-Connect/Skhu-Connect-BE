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

    private JwtDecoder jwtDecoder;
    private AccessTokenAuthenticationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtDecoder = mock(JwtDecoder.class);
        filter = new AccessTokenAuthenticationFilter(jwtDecoder);
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
    void nonPetitionRequestDoesNotRequireToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/connect/departments");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtDecoder, never()).decode(org.mockito.ArgumentMatchers.anyString());
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
