package org.skhuconnect.admin.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.skhuconnect.admin.content.dto.AdminCommentResponse;
import org.skhuconnect.admin.content.dto.AdminContentHideRequest;
import org.skhuconnect.admin.content.dto.AdminPageResponse;
import org.skhuconnect.admin.content.dto.AdminPetitionResponse;
import org.skhuconnect.admin.content.service.AdminContentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 콘텐츠", description = "관리자 청원·댓글 관리 API")
@RestController
@RequestMapping("/connect/admin/petitions")
public class AdminContentController {

    private final AdminContentService service;

    public AdminContentController(AdminContentService service) {
        this.service = service;
    }

    @Operation(summary = "관리자용 청원 목록 조회")
    @GetMapping
    public AdminPageResponse<AdminPetitionResponse> findPetitions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.findPetitions(page, size);
    }

    @Operation(summary = "청원 숨김")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Petition hidden"),
            @ApiResponse(responseCode = "400", description = "Invalid reason", content = @Content),
            @ApiResponse(responseCode = "404", description = "Petition or administrator not found", content = @Content)
    })
    @PatchMapping("/{petitionId}/hide")
    public AdminPetitionResponse hidePetition(
            @RequestAttribute("adminId") Long adminId,
            @PathVariable Long petitionId,
            @Valid @RequestBody AdminContentHideRequest request
    ) {
        return service.hidePetition(adminId, petitionId, request);
    }

    @Operation(summary = "청원 숨김 복구")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Petition restored"),
            @ApiResponse(responseCode = "404", description = "Petition not found", content = @Content)
    })
    @PatchMapping("/{petitionId}/restore")
    public AdminPetitionResponse restorePetition(@RequestAttribute("adminId") Long adminId, @PathVariable Long petitionId) {
        return service.restorePetition(adminId, petitionId);
    }

    @Operation(summary = "관리자용 댓글·대댓글 목록 조회")
    @GetMapping("/{petitionId}/comments")
    public AdminPageResponse<AdminCommentResponse> findComments(
            @PathVariable Long petitionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.findComments(petitionId, page, size);
    }

    @Operation(summary = "댓글·대댓글 숨김")
    @PatchMapping("/{petitionId}/comments/{commentId}/hide")
    public AdminCommentResponse hideComment(
            @RequestAttribute("adminId") Long adminId,
            @PathVariable Long petitionId,
            @PathVariable Long commentId,
            @Valid @RequestBody AdminContentHideRequest request
    ) {
        return service.hideComment(adminId, petitionId, commentId, request);
    }

    @Operation(summary = "댓글·대댓글 숨김 복구")
    @PatchMapping("/{petitionId}/comments/{commentId}/restore")
    public AdminCommentResponse restoreComment(
            @RequestAttribute("adminId") Long adminId,
            @PathVariable Long petitionId,
            @PathVariable Long commentId
    ) {
        return service.restoreComment(adminId, petitionId, commentId);
    }
}