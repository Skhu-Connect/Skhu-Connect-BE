package org.skhuconnect.bookmark.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.skhuconnect.bookmark.dto.response.BookmarkPageResponse;
import org.skhuconnect.bookmark.dto.response.BookmarkResponse;
import org.skhuconnect.bookmark.service.BookmarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bookmark", description = "청원 북마크 API")
@RestController
@RequestMapping("/connect/petitions")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @Operation(summary = "청원 북마크 등록")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "북마크 등록 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 또는 청원 없음",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "중복 북마크",
                    content = @Content)
    })
    @PostMapping("/{petitionId}/bookmarks")
    public ResponseEntity<BookmarkResponse> create(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long petitionId
    ) {
        return ResponseEntity.status(201)
                .body(bookmarkService.create(userId, petitionId));
    }

    @Operation(summary = "청원 북마크 취소")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "북마크 취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "404", description = "청원 또는 북마크 없음",
                    content = @Content)
    })
    @DeleteMapping("/{petitionId}/bookmarks")
    public ResponseEntity<Void> cancel(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long petitionId
    ) {
        bookmarkService.cancel(userId, petitionId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내 북마크 청원 목록 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "북마크 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 페이지 요청",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content)
    })
    @GetMapping("/bookmarks")
    public BookmarkPageResponse findMine(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return bookmarkService.findMine(userId, page, size);
    }
}
