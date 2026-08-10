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

@Tag(name = "愿由ъ옄 ?몄쬆", description = "愿由ъ옄 ?몄쬆 API")
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

    @Operation(summary = "愿由ъ옄 濡쒓렇??)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "濡쒓렇???깃났"),
            @ApiResponse(responseCode = "401", description = "?몄쬆 ?뺣낫媛 ?щ컮瑜댁? ?딆뒿?덈떎", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return tokenResponse(adminAuthService.login(request.loginId(), request.password()));
    }

    @Operation(summary = "愿由ъ옄 ?≪꽭???좏겙 ?щ컻湲?)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "?좏겙 ?щ컻湲??깃났"),
            @ApiResponse(responseCode = "401", description = "由ы봽?덉떆 ?좏겙???щ컮瑜댁? ?딆뒿?덈떎", content = @Content),
            @ApiResponse(responseCode = "410", description = "由ы봽?덉떆 ?좏겙??留뚮즺?섏뿀?듬땲??, content = @Content)
    })
    @PostMapping("/token/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = AdminRefreshTokenCookieService.COOKIE_NAME,
                    required = false) String refreshToken) {
        return tokenResponse(adminAuthService.refresh(refreshToken));
    }

    @Operation(summary = "愿由ъ옄 濡쒓렇?꾩썐")
    @ApiResponse(responseCode = "204", description = "濡쒓렇?꾩썐 ?깃났")
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