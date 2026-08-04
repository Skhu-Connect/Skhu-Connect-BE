package org.skhuconnect.auth.token.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.skhuconnect.auth.token.dto.AccessTokenResponse;
import org.skhuconnect.auth.token.dto.LoginRequest;
import org.skhuconnect.auth.token.dto.TokenIssueResult;
import org.skhuconnect.auth.token.service.RefreshTokenCookieService;
import org.skhuconnect.auth.token.service.UserAuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "사용자 인증 API")
@RestController
@RequestMapping("/connect/auth")
public class UserAuthController {

    private final UserAuthService userAuthService;
    private final RefreshTokenCookieService cookieService;

    public UserAuthController(
            UserAuthService userAuthService,
            RefreshTokenCookieService cookieService) {
        this.userAuthService = userAuthService;
        this.cookieService = cookieService;
    }

    @Operation(summary = "사용자 로그인")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 불일치", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(
            @Valid @RequestBody LoginRequest request) {
        TokenIssueResult result = userAuthService.login(
                request.loginId(), request.password());
        return tokenResponse(result);
    }

    @Operation(summary = "Access Token 재발급")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "Refresh Token 무효", content = @Content),
            @ApiResponse(responseCode = "410", description = "Refresh Token 만료", content = @Content)
    })
    @PostMapping("/token/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = RefreshTokenCookieService.COOKIE_NAME,
                    required = false) String refreshToken) {
        return tokenResponse(userAuthService.refresh(refreshToken));
    }

    @Operation(summary = "사용자 로그아웃")
    @ApiResponse(responseCode = "204", description = "로그아웃 완료")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookieService.COOKIE_NAME,
                    required = false) String refreshToken) {
        userAuthService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieService.expire().toString())
                .build();
    }

    private ResponseEntity<AccessTokenResponse> tokenResponse(
            TokenIssueResult result) {
        AccessTokenResponse response = new AccessTokenResponse(
                result.accessToken(), result.expiresInSeconds());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookieService.issue(result.refreshToken()).toString())
                .body(response);
    }
}
