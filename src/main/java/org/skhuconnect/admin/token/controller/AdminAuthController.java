package org.skhuconnect.admin.token.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.skhuconnect.admin.token.service.AdminAuthService;
import org.skhuconnect.admin.token.service.AdminRefreshTokenCookieService;
import org.skhuconnect.auth.token.dto.AccessTokenResponse;
import org.skhuconnect.auth.token.dto.LoginRequest;
import org.skhuconnect.auth.token.dto.TokenIssueResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Authentication", description = "관리자 인증 API")
@RestController
@RequestMapping("/connect/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final AdminRefreshTokenCookieService cookieService;

    public AdminAuthController(AdminAuthService adminAuthService,
                               AdminRefreshTokenCookieService cookieService) {
        this.adminAuthService = adminAuthService;
        this.cookieService = cookieService;
    }

    @Operation(summary = "관리자 로그인")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return tokenResponse(adminAuthService.login(request.loginId(), request.password()));
    }

    @Operation(summary = "관리자 액세스 토큰 재발급")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refresh successful"),
            @ApiResponse(responseCode = "401", description = "Invalid refresh token", content = @Content),
            @ApiResponse(responseCode = "410", description = "Expired refresh token", content = @Content)
    })
    @PostMapping("/token/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = AdminRefreshTokenCookieService.COOKIE_NAME,
                    required = false) String refreshToken) {
        return tokenResponse(adminAuthService.refresh(refreshToken));
    }

    @Operation(summary = "관리자 로그아웃")
    @ApiResponse(responseCode = "204", description = "Logout successful")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = AdminRefreshTokenCookieService.COOKIE_NAME,
                    required = false) String refreshToken) {
        adminAuthService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieService.expire().toString())
                .build();
    }

    private ResponseEntity<AccessTokenResponse> tokenResponse(TokenIssueResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.issue(result.refreshToken()).toString())
                .body(new AccessTokenResponse(result.accessToken(), result.expiresInSeconds()));
    }
}