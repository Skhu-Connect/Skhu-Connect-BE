package org.skhuconnect.petition.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.skhuconnect.petition.dto.request.PetitionCreateRequest;
import org.skhuconnect.petition.dto.request.PetitionUpdateRequest;
import org.skhuconnect.petition.dto.response.PetitionResponse;
import org.skhuconnect.petition.service.PetitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Petition", description = "사용자 청원 API")
@RestController
@RequestMapping("/connect/petitions")
public class PetitionController {

    private final PetitionService petitionService;

    public PetitionController(PetitionService petitionService) {
        this.petitionService = petitionService;
    }

    @Operation(summary = "청원 등록")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "청원 등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 또는 임계치 설정 없음",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<PetitionResponse> create(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody PetitionCreateRequest request
    ) {
        return ResponseEntity.status(201).body(petitionService.create(userId, request));
    }

    @Operation(summary = "청원 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "청원 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류", content = @Content),
            @ApiResponse(responseCode = "403", description = "작성자가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "청원 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "수정할 수 없는 청원",
                    content = @Content)
    })
    @PutMapping("/{petitionId}")
    public PetitionResponse update(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long petitionId,
            @Valid @RequestBody PetitionUpdateRequest request
    ) {
        return petitionService.update(userId, petitionId, request);
    }

    @Operation(summary = "청원 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "청원 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "작성자가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "청원 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "삭제할 수 없는 청원",
                    content = @Content)
    })
    @DeleteMapping("/{petitionId}")
    public ResponseEntity<Void> delete(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long petitionId
    ) {
        petitionService.delete(userId, petitionId);
        return ResponseEntity.noContent().build();
    }
}
