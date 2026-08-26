package org.skhuconnect.auth.loginid.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.skhuconnect.auth.loginid.dto.LoginIdFindEmailRequest;
import org.skhuconnect.auth.loginid.dto.LoginIdFindPasswordRequest;
import org.skhuconnect.auth.loginid.dto.LoginIdResponse;
import org.skhuconnect.auth.loginid.service.LoginIdFindService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "사용자 인증 API")
@RestController
@RequestMapping("/connect/auth/login-id/find")
public class LoginIdFindController {

    private final LoginIdFindService service;

    public LoginIdFindController(LoginIdFindService service) {
        this.service = service;
    }

    @Operation(
            summary = "이메일 인증으로 아이디 찾기",
            description = "LOGIN_ID_FIND 목적으로 발급된 verificationToken을 한 번 소비하고 전체 로그인 아이디를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "아이디 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginIdResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 token 또는 인증 목적 불일치", content = @Content),
            @ApiResponse(responseCode = "401", description = "사용자 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 사용된 token", content = @Content),
            @ApiResponse(responseCode = "410", description = "만료된 token", content = @Content)
    })
    @PostMapping("/email")
    public LoginIdResponse findByEmailVerification(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(
                            value = "{\"verificationToken\":\"verification-token\"}")))
            @Valid @RequestBody LoginIdFindEmailRequest request
    ) {
        return service.findByEmailVerification(request);
    }

    @Operation(
            summary = "이메일과 현재 비밀번호로 아이디 찾기",
            description = "이메일이 없거나 비밀번호가 틀리면 동일한 401 응답을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "아이디 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginIdResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치", content = @Content)
    })
    @PostMapping("/password")
    public LoginIdResponse findByPassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(
                            value = "{\"email\":\"20260000@office.skhu.ac.kr\",\"password\":\"current-password\"}")))
            @Valid @RequestBody LoginIdFindPasswordRequest request
    ) {
        return service.findByPassword(request);
    }
}
