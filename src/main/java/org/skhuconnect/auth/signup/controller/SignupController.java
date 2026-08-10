package org.skhuconnect.auth.signup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.skhuconnect.auth.signup.dto.SignupRequest;
import org.skhuconnect.auth.signup.service.SignupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "사용자 인증 API")
@RestController
@RequestMapping("/connect/auth")
public class SignupController {

    private final SignupService signupService;

    public SignupController(SignupService signupService) {
        this.signupService = signupService;
    }

    @Operation(summary = "사용자 회원가입")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "요청 또는 인증 token 오류", content = @Content),
            @ApiResponse(responseCode = "404", description = "학과 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "중복 계정, 사용된 token 또는 탈퇴 후 30일 이내 재가입", content = @Content),
            @ApiResponse(responseCode = "410", description = "인증 token 만료", content = @Content)
    })
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {
        signupService.signup(request);
        return ResponseEntity.status(201).build();
    }
}