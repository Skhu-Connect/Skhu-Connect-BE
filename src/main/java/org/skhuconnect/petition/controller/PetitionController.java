package org.skhuconnect.petition.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.skhuconnect.petition.dto.request.PetitionCreateRequest;
import org.skhuconnect.petition.dto.request.PetitionQueryCondition;
import org.skhuconnect.petition.dto.request.PetitionUpdateRequest;
import org.skhuconnect.petition.dto.response.PetitionPageResponse;
import org.skhuconnect.petition.dto.response.PetitionQueryResponse;
import org.skhuconnect.petition.dto.response.PetitionResponse;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.entity.PetitionStatus;
import org.skhuconnect.petition.service.PetitionService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.data.domain.Sort.Direction.DESC;

@Tag(name = "Petition", description = "사용자 청원 API")
@RestController
@RequestMapping("/connect/petitions")
public class PetitionController {

    private final PetitionService petitionService;

    public PetitionController(PetitionService petitionService) {
        this.petitionService = petitionService;
    }

    @Operation(summary = "청원 등록", description = "청원 등록 성공 시점부터 10분 후 다시 등록할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "청원 등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 또는 임계치 설정 없음",
                    content = @Content),
            @ApiResponse(responseCode = "429", description = "청원 등록 후 10분이 지나지 않음",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<PetitionResponse> create(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody PetitionCreateRequest request
    ) {
        return ResponseEntity.status(201).body(petitionService.create(userId, request));
    }

    @SecurityRequirements
    @Operation(summary = "청원 목록 및 검색 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "청원 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "필터 또는 정렬 값 오류",
                    content = @Content)
    })
    @GetMapping
    public PetitionPageResponse findAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PetitionCategory category,
            @RequestParam(required = false) PetitionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return petitionService.findAll(new PetitionQueryCondition(
                keyword, category, status, page, size, sort));
    }

    @SecurityRequirements
    @Operation(
            summary = "청원 상세 조회",
            description = "공유받은 사용자도 로그인 없이 청원 본문을 조회할 수 있습니다. "
                    + "사용자 삭제, 관리자 숨김 또는 존재하지 않는 청원은 404를 반환합니다. "
                    + "동의와 댓글 작성 등 상태 변경은 별도 인증 API를 사용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "청원 본문, 상태, 동의 수, 마감 시각 및 공식 답변 조회 성공"),
            @ApiResponse(responseCode = "404", description = "청원이 없거나 사용자 삭제 또는 관리자 숨김 상태", content = @Content)
    })
    @GetMapping("/{petitionId}")
    public PetitionQueryResponse findDetail(@PathVariable Long petitionId) {
        return petitionService.findDetail(petitionId);
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