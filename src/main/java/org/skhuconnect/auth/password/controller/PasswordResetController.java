package org.skhuconnect.auth.password.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.skhuconnect.auth.password.dto.PasswordResetRequest;
import org.skhuconnect.auth.password.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "사용자 인증 API")
@RestController
@RequestMapping("/connect/auth/password")
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @Operation(summary = "사용자 비밀번호 재설정")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "비밀번호 재설정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 token 또는 인증 목적 불일치", content = @Content),
            @ApiResponse(responseCode = "404", description = "인증 이메일에 대응하는 사용자 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 사용된 token", content = @Content),
            @ApiResponse(responseCode = "410", description = "만료된 token", content = @Content)
    })
    @PostMapping("/reset")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
