package org.skhuconnect.auth.email.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.skhuconnect.auth.email.dto.request.EmailVerificationConfirmRequest;
import org.skhuconnect.auth.email.dto.request.EmailVerificationSendRequest;
import org.skhuconnect.auth.email.dto.response.EmailVerificationConfirmResponse;
import org.skhuconnect.auth.email.service.EmailVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/connect/auth/email-verifications")
@Tag(name = "Authentication", description = "사용자 이메일 인증 API")
public class EmailVerificationController {
    private final EmailVerificationService service;

    public EmailVerificationController(EmailVerificationService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "인증번호 발송",
            description = "성공회대 공식 이메일로 6자리 인증번호를 발송합니다.")
    @ApiResponse(responseCode = "204", description = "인증번호 발송 성공")
    public ResponseEntity<Void> send(
            @Valid @RequestBody EmailVerificationSendRequest request) {
        service.sendCode(request.email(), request.purpose());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/confirm")
    @Operation(summary = "인증번호 확인",
            description = "인증번호를 확인하고 30분간 유효한 일회용 인증 토큰을 발급합니다.")
    @ApiResponse(responseCode = "200", description = "인증번호 확인 성공")
    public EmailVerificationConfirmResponse confirm(
            @Valid @RequestBody EmailVerificationConfirmRequest request) {
        return service.confirm(request.email(), request.code(), request.purpose());
    }
}
