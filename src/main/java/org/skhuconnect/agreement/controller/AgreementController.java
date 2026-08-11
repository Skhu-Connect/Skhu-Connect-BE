package org.skhuconnect.agreement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.skhuconnect.agreement.dto.response.AgreementResponse;
import org.skhuconnect.agreement.service.AgreementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agreement", description = "청원 동의 API")
@RestController
@RequestMapping("/connect/petitions/{petitionId}/agreements")
public class AgreementController {

    private final AgreementService agreementService;

    public AgreementController(AgreementService agreementService) {
        this.agreementService = agreementService;
    }

    @Operation(summary = "청원 동의")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "청원 동의 성공"),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 또는 청원 없음",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "동의 불가 또는 중복 동의",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<AgreementResponse> agree(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long petitionId
    ) {
        return ResponseEntity.status(201)
                .body(agreementService.agree(userId, petitionId));
    }

    @Operation(summary = "청원 동의 취소")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "청원 동의 취소 성공"),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content),
            @ApiResponse(responseCode = "404", description = "청원 또는 동의 없음",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "동의 취소 불가",
                    content = @Content)
    })
    @DeleteMapping
    public ResponseEntity<Void> cancel(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long petitionId
    ) {
        agreementService.cancel(userId, petitionId);
        return ResponseEntity.noContent().build();
    }
}