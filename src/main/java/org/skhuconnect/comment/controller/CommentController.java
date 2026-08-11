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

    @Operation(summary = "댓글 또는 답글 작성", description = "로그인이 필요합니다. parentCommentId가 없으면 댓글, 있으면 해당 댓글의 답글을 작성합니다.")
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

    @Operation(
            summary = "댓글과 답글 목록 조회",
            description = "공유받은 사용자도 로그인 없이 조회할 수 있습니다. 답글은 각 댓글의 replies에 포함됩니다. "
                    + "비로그인 응답의 myComment와 liked는 false입니다. 청원이 없거나 삭제·숨김 상태이면 404를 반환합니다."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "익명 번호, 내용, 공감 수, 작성자 여부, 공감 여부와 답글 목록 조회 성공"),
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

    @Operation(summary = "댓글 또는 답글 수정", description = "로그인한 작성자만 수정할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "작성자가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "청원 또는 댓글 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "수정할 수 없는 댓글", content = @Content)
    })
    @PutMapping("/{commentId}")
    public CommentResponse update(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long petitionId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        return commentService.update(userId, petitionId, commentId, request);
    }

    @Operation(summary = "댓글 또는 답글 삭제", description = "로그인한 작성자만 삭제할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "작성자가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "청원 또는 댓글 없음", content = @Content)
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long petitionId,
            @PathVariable Long commentId
    ) {
        commentService.delete(userId, petitionId, commentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "댓글 또는 답글 공감", description = "로그인이 필요하며 자동 공감은 수행하지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "공감 성공"),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자, 청원 또는 댓글 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "공감 불가 또는 중복 공감", content = @Content)
    })
    @PostMapping("/{commentId}/likes")
    public ResponseEntity<CommentLikeResponse> like(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long petitionId,
            @PathVariable Long commentId
    ) {
        return ResponseEntity.status(201)
                .body(commentLikeService.like(userId, petitionId, commentId));
    }

    @Operation(summary = "댓글 또는 답글 공감 취소", description = "로그인이 필요합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공감 취소 성공"),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자, 청원, 댓글 또는 기존 공감 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "공감 취소 불가", content = @Content)
    })
    @DeleteMapping("/{commentId}/likes")
    public CommentLikeResponse cancelLike(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long petitionId,
            @PathVariable Long commentId
    ) {
        return commentLikeService.cancel(userId, petitionId, commentId);
    }
}
