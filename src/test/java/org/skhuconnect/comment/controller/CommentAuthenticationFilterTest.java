package org.skhuconnect.comment.controller;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.global.security.AccessTokenAuthenticationFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentAuthenticationFilterTest {

    private JwtDecoder jwtDecoder;
    private AccessTokenAuthenticationFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        jwtDecoder = mock(JwtDecoder.class);
        org.skhuconnect.user.repository.UserRepository users =
                mock(org.skhuconnect.user.repository.UserRepository.class);
        filter = new AccessTokenAuthenticationFilter(jwtDecoder, users);
        when(users.existsByIdAndDeletedFalse(org.mockito.ArgumentMatchers.anyLong())).thenReturn(true);
        chain = mock(FilterChain.class);
    }

    @Test
    void commentListWithoutTokenIsPublic() throws Exception {
        MockHttpServletRequest request = request("GET", "/connect/petitions/10/comments");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(jwtDecoder, never()).decode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void commentListWithTokenValidatesAndInjectsUserId() throws Exception {
        MockHttpServletRequest request = request("GET", "/connect/petitions/10/comments");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtDecoder.decode("token")).thenReturn(Jwt.withTokenValue("token")
                .header("alg", "HS256").subject("42").claim("role", "USER").build());

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute(AccessTokenAuthenticationFilter.USER_ID_ATTRIBUTE))
                .isEqualTo(42L);
        verify(chain).doFilter(request, response);
    }

    @Test
    void commentListWithInvalidBearerTokenReturnsUnauthorized() throws Exception {
        MockHttpServletRequest request = request("GET", "/connect/petitions/10/comments");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtDecoder.decode("invalid")).thenThrow(
                new org.springframework.security.oauth2.jwt.JwtValidationException(
                        "invalid", java.util.List.of(
                        new org.springframework.security.oauth2.core.OAuth2Error(
                                "invalid_token"))));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }
    @Test
    void malformedAuthorizationAndCommentWritesReturnUnauthorized() throws Exception {
        MockHttpServletRequest malformed = request("GET", "/connect/petitions/10/comments");
        malformed.addHeader(HttpHeaders.AUTHORIZATION, "invalid");
        MockHttpServletResponse malformedResponse = new MockHttpServletResponse();
        filter.doFilter(malformed, malformedResponse, chain);
        assertThat(malformedResponse.getStatus()).isEqualTo(401);

        for (String method : new String[]{"POST", "PUT", "DELETE"}) {
            MockHttpServletRequest write = request(
                    method, "/connect/petitions/10/comments/5");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(write, response, chain);
            assertThat(response.getStatus()).isEqualTo(401);
        }
    }

    private MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }
}
