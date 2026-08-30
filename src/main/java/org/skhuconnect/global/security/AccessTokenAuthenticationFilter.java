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
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AccessTokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID_ATTRIBUTE = "userId";
    public static final String ADMIN_ID_ATTRIBUTE = "adminId";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ROLE = "USER";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PETITION_PATH = "/connect/petitions";
    private static final String NOTIFICATION_PATH = "/connect/notifications";
    private static final String USER_PATH = "/connect/users";
    private static final String REPORT_PATH = "/connect/reports";
    private static final String ADMIN_PATH = "/connect/admin";
    private static final String ADMIN_AUTH_PATH = "/connect/admin/auth";

    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;

    public AccessTokenAuthenticationFilter(
            JwtDecoder jwtDecoder, UserRepository userRepository) {
        this.jwtDecoder = jwtDecoder;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path.equals(ADMIN_PATH) || path.startsWith(ADMIN_PATH + "/")) {
            return path.equals(ADMIN_AUTH_PATH) || path.startsWith(ADMIN_AUTH_PATH + "/");
        }
        boolean petitionPath = path.equals(PETITION_PATH)
                || path.startsWith(PETITION_PATH + "/");
        boolean notificationPath = path.equals(NOTIFICATION_PATH)
                || path.startsWith(NOTIFICATION_PATH + "/");
        boolean userPath = path.equals(USER_PATH)
                || path.startsWith(USER_PATH + "/");
        boolean reportPath = path.equals(REPORT_PATH);
        if (!petitionPath && !notificationPath && !userPath && !reportPath) {
            return true;
        }
        if (notificationPath || userPath) {
            return false;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        boolean adminRequest = isAdminRequest(request);
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            if (!adminRequest && isPublicGet(request) && authorization == null) {
                filterChain.doFilter(request, response);
                return;
            }
            unauthorized(response);
            return;
        }

        try {
            Jwt jwt = jwtDecoder.decode(authorization.substring(BEARER_PREFIX.length()));
            String expectedRole = adminRequest ? ADMIN_ROLE : USER_ROLE;
            if (!expectedRole.equals(jwt.getClaimAsString("role"))) {
                unauthorized(response);
                return;
            }
            Long subjectId = Long.valueOf(jwt.getSubject());
            if (subjectId <= 0) {
                unauthorized(response);
                return;
            }
            request.setAttribute(adminRequest ? ADMIN_ID_ATTRIBUTE : USER_ID_ATTRIBUTE, subjectId);
            if (!adminRequest && !userRepository.existsByIdAndDeletedFalseAndLoginBannedFalse(subjectId)) {
                unauthorized(response);
                return;
            }
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException exception) {
            unauthorized(response);
        }
    }

    private boolean isAdminRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals(ADMIN_PATH) || path.startsWith(ADMIN_PATH + "/");
    }

    private boolean isPublicGet(HttpServletRequest request) {
        return HttpMethod.GET.matches(request.getMethod())
                && (request.getRequestURI().equals(PETITION_PATH)
                || request.getRequestURI().matches(PETITION_PATH + "/\\d+")
                || request.getRequestURI().matches(PETITION_PATH + "/\\d+/comments"));
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(
                "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401}");
    }
}
