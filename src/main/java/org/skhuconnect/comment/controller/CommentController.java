package org.skhuconnect.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.skhuconnect.comment.dto.request.CommentCreateRequest;
import org.skhuconnect.comment.dto.request.CommentUpdateRequest;
import org.skhuconnect.comment.dto.response.CommentLikeResponse;
import org.skhuconnect.comment.dto.response.CommentPageResponse;
import org.skhuconnect.comment.dto.response.CommentResponse;
import org.skhuconnect.comment.service.CommentLikeService;
import org.skhuconnect.comment.service.CommentService;
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

@Tag(name = "Comment", description = "청원 댓글 및 댓글 공감 API")
@RestController
@RequestMapping("/connect/petitions/{petitionId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final CommentLikeService commentLikeService;

    public CommentController(
            CommentService commentService,
            CommentLikeService commentLikeService
    ) {
        this.commentService = commentService;
        this.commentLikeService = commentLikeService;
    }

    @Operation(summary = "댓글 작성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "댓글 작성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 또는 청원 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "댓글 작성 불가 또는 익명 번호 충돌", content = @Content)
    })
    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long petitionId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        return ResponseEntity.status(201)
                .body(commentService.create(userId, petitionId, request));
    }

    @Operation(summary = "댓글 목록 조회")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 페이지 요청", content = @Content),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 선택적 토큰", content = @Content),
            @ApiResponse(responseCode = "404", description = "청원 없음", content = @Content)
    })
    @GetMapping
    public CommentPageResponse findAll(
            @Parameter(hidden = true)
            @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long petitionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return commentService.findAll(userId, petitionId, page, size);
    }

    @Operation(summary = "댓글 수정")
    @PutMapping("/{commentId}")
    public CommentResponse update(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long petitionId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        return commentService.update(userId, petitionId, commentId, request);
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long petitionId,
            @PathVariable Long commentId
    ) {
        commentService.delete(userId, petitionId, commentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "댓글 공감")
    @PostMapping("/{commentId}/likes")
    public ResponseEntity<CommentLikeResponse> like(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long petitionId,
            @PathVariable Long commentId
    ) {
        return ResponseEntity.status(201)
                .body(commentLikeService.like(userId, petitionId, commentId));
    }

    @Operation(summary = "댓글 공감 취소")
    @DeleteMapping("/{commentId}/likes")
    public CommentLikeResponse cancelLike(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long petitionId,
            @PathVariable Long commentId
    ) {
        return commentLikeService.cancel(userId, petitionId, commentId);
    }
}
