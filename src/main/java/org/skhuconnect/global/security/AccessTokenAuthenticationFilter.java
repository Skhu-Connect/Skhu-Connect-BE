package org.skhuconnect.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AccessTokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID_ATTRIBUTE = "userId";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ROLE = "USER";
    private static final String PETITION_PATH = "/connect/petitions";

    private final JwtDecoder jwtDecoder;

    public AccessTokenAuthenticationFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean petitionPath = path.equals(PETITION_PATH)
                || path.startsWith(PETITION_PATH + "/");
        if (!petitionPath) {
            return true;
        }
        if (HttpMethod.GET.matches(request.getMethod())) {
            return path.equals(PETITION_PATH)
                    || path.matches(PETITION_PATH + "/\\d+");
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            if (isPublicCommentListGet(request) && authorization == null) {
                filterChain.doFilter(request, response);
                return;
            }
            unauthorized(response);
            return;
        }

        try {
            Jwt jwt = jwtDecoder.decode(authorization.substring(BEARER_PREFIX.length()));
            if (!USER_ROLE.equals(jwt.getClaimAsString("role"))) {
                unauthorized(response);
                return;
            }
            Long userId = Long.valueOf(jwt.getSubject());
            if (userId <= 0) {
                unauthorized(response);
                return;
            }
            request.setAttribute(USER_ID_ATTRIBUTE, userId);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException exception) {
            unauthorized(response);
        }
    }

    private boolean isPublicCommentListGet(HttpServletRequest request) {
        return HttpMethod.GET.matches(request.getMethod())
                && request.getRequestURI().matches(
                        PETITION_PATH + "/\\d+/comments");
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(
                "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401}");
    }
}