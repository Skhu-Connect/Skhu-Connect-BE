package org.skhuconnect.bookmark.controller;

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

class BookmarkAuthenticationTest {

    private JwtDecoder jwtDecoder;
    private AccessTokenAuthenticationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtDecoder = mock(JwtDecoder.class);
        org.skhuconnect.user.repository.UserRepository users =
                mock(org.skhuconnect.user.repository.UserRepository.class);
        filter = new AccessTokenAuthenticationFilter(jwtDecoder, users);
        when(users.existsByIdAndDeletedFalseAndLoginBannedFalse(org.mockito.ArgumentMatchers.anyLong())).thenReturn(true);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void bookmarkListRequiresTokenAndValidTokenInjectsUserId() throws Exception {
        MockHttpServletRequest missing = new MockHttpServletRequest(
                "GET", "/connect/petitions/bookmarks");
        MockHttpServletResponse missingResponse = new MockHttpServletResponse();

        filter.doFilter(missing, missingResponse, filterChain);
        assertThat(missingResponse.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(missing, missingResponse);

        MockHttpServletRequest valid = new MockHttpServletRequest(
                "GET", "/connect/petitions/bookmarks");
        valid.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        MockHttpServletResponse validResponse = new MockHttpServletResponse();
        when(jwtDecoder.decode("valid-token")).thenReturn(Jwt.withTokenValue("token")
                .header("alg", "HS256").subject("42").claim("role", "USER").build());

        filter.doFilter(valid, validResponse, filterChain);
        assertThat(valid.getAttribute(AccessTokenAuthenticationFilter.USER_ID_ATTRIBUTE))
                .isEqualTo(42L);
        verify(filterChain).doFilter(valid, validResponse);
    }

    @Test
    void existingPublicPetitionGetsRemainPublic() throws Exception {
        for (String path : new String[]{"/connect/petitions", "/connect/petitions/10"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, filterChain);
            verify(filterChain).doFilter(request, response);
        }
    }
}
