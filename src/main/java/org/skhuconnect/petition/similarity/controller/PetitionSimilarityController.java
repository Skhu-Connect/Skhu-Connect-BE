package org.skhuconnect.petition.similarity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.skhuconnect.petition.similarity.dto.PetitionSimilarityRequest;
import org.skhuconnect.petition.similarity.dto.PetitionSimilarityResponse;
import org.skhuconnect.petition.similarity.dto.PetitionSimilarityUsageResponse;
import org.skhuconnect.petition.similarity.service.PetitionSimilarityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Petition Similarity", description = "AI 기반 유사 청원 탐지 API")
@RestController
@RequestMapping("/connect/petitions/similar")
public class PetitionSimilarityController {

    private final PetitionSimilarityService petitionSimilarityService;

    public PetitionSimilarityController(PetitionSimilarityService petitionSimilarityService) {
        this.petitionSimilarityService = petitionSimilarityService;
    }

    @Operation(
            summary = "AI 유사 청원 찾기",
            description = "로그인 사용자가 글 작성 중 제목과 본문을 기준으로 공개 청원 전체에서 의미적으로 유사한 글을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "유사 청원 조회 성공. 결과가 없으면 빈 목록을 반환합니다."),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content),
            @ApiResponse(responseCode = "429", description = "10분 내 새 검색 횟수 초과", content = @Content),
            @ApiResponse(responseCode = "503", description = "AI 임베딩 서비스 장애", content = @Content)
    })
    @PostMapping
    public PetitionSimilarityResponse findSimilar(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody PetitionSimilarityRequest request
    ) {
        return petitionSimilarityService.findSimilar(userId, request);
    }

    @Operation(
            summary = "AI 유사 청원 검색 사용량 조회",
            description = "최근 10분 기준 남은 새 검색 횟수와 재사용 가능 시간을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용량 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content)
    })
    @GetMapping("/usage")
    public PetitionSimilarityUsageResponse findUsage(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId
    ) {
        return petitionSimilarityService.findUsage(userId);
    }
}
