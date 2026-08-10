package org.skhuconnect.admin.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.skhuconnect.admin.content.dto.*;
import org.skhuconnect.admin.content.service.AdminContentService;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 콘텐츠", description = "관리자 청원·댓글 관리 API")
@RestController
@RequestMapping("/connect/admin/petitions")
public class AdminContentController {
    private final AdminContentService service;
    public AdminContentController(AdminContentService service) { this.service = service; }
    @Operation(summary = "관리자용 청원 목록 조회")
    @GetMapping public AdminPageResponse<AdminPetitionResponse> findPetitions(@RequestParam(defaultValue = "0") int page,@RequestParam(defaultValue = "20") int size){return service.findPetitions(page,size);}
    @Operation(summary = "청원 숨김")
    @ApiResponses({@ApiResponse(responseCode="200",description="청원 숨김 성공"),@ApiResponse(responseCode="400",description="숨김 사유 오류",content=@Content),@ApiResponse(responseCode="404",description="청원 또는 관리자 없음",content=@Content)})
    @PatchMapping("/{petitionId}/hide") public AdminPetitionResponse hidePetition(@RequestAttribute("adminId") Long adminId,@PathVariable Long petitionId,@Valid @RequestBody AdminContentHideRequest request){return service.hidePetition(adminId,petitionId,request);}
    @Operation(summary = "청원 숨김 복구")
    @ApiResponses({@ApiResponse(responseCode="200",description="청원 복구 성공"),@ApiResponse(responseCode="404",description="청원 없음",content=@Content)})
    @PatchMapping("/{petitionId}/restore") public AdminPetitionResponse restorePetition(@RequestAttribute("adminId") Long adminId,@PathVariable Long petitionId){return service.restorePetition(adminId,petitionId);}
    @Operation(summary = "관리자용 댓글·대댓글 목록 조회")
    @GetMapping("/{petitionId}/comments") public AdminPageResponse<AdminCommentResponse> findComments(@PathVariable Long petitionId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.findComments(petitionId,page,size);}
    @Operation(summary = "댓글·대댓글 숨김")
    @PatchMapping("/{petitionId}/comments/{commentId}/hide") public AdminCommentResponse hideComment(@RequestAttribute("adminId") Long adminId,@PathVariable Long petitionId,@PathVariable Long commentId,@Valid @RequestBody AdminContentHideRequest request){return service.hideComment(adminId,petitionId,commentId,request);}
    @Operation(summary = "댓글·대댓글 숨김 복구")
    @PatchMapping("/{petitionId}/comments/{commentId}/restore") public AdminCommentResponse restoreComment(@RequestAttribute("adminId") Long adminId,@PathVariable Long petitionId,@PathVariable Long commentId){return service.restoreComment(adminId,petitionId,commentId);}
}