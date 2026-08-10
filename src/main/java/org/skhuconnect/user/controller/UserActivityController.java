package org.skhuconnect.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.skhuconnect.notification.dto.NotificationPageResponse;
import org.skhuconnect.petition.dto.response.PetitionPageResponse;
import org.skhuconnect.user.dto.UserCommentPageResponse;
import org.skhuconnect.user.dto.UserMeResponse;
import org.skhuconnect.user.service.UserActivityService;
import org.skhuconnect.user.dto.UserWithdrawalRequest;
import org.skhuconnect.user.service.UserWithdrawalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Activity", description = "로그인 사용자 본인 정보 및 활동 내역 API")
@RestController
@RequestMapping("/connect/users/me")
public class UserActivityController {

    private final UserActivityService service;
    private final UserWithdrawalService withdrawalService;

    public UserActivityController(
            UserActivityService service, UserWithdrawalService withdrawalService) {
        this.service = service;
        this.withdrawalService = withdrawalService;
    }

    @Operation(summary = "내 정보 조회")
    @GetMapping
    public UserMeResponse findMe(
            @Parameter(hidden = true)
            @RequestAttribute("userId") Long userId
    ) {
        return service.findMe(userId);
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "현재 비밀번호를 재확인한 뒤 탈퇴합니다. 동일 학교 이메일은 탈퇴 후 30일 동안 재가입할 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "회원 탈퇴 성공"),
            @ApiResponse(responseCode = "400", description = "현재 비밀번호 누락", content = @Content),
            @ApiResponse(responseCode = "401", description = "Access Token 무효 또는 비밀번호 불일치", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content)
    })
    @DeleteMapping
    public ResponseEntity<Void> withdraw(
            @Parameter(hidden = true)
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody UserWithdrawalRequest request
    ) {
        withdrawalService.withdraw(userId, request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내가 작성한 청원 조회")
    @GetMapping("/petitions")
    public PetitionPageResponse findMyPetitions(
            @Parameter(hidden = true)
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.findMyPetitions(userId, page, size);
    }

    @Operation(summary = "내가 동의한 청원 조회")
    @GetMapping("/agreements")
    public PetitionPageResponse findMyAgreements(
            @Parameter(hidden = true)
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.findMyAgreements(userId, page, size);
    }

    @Operation(summary = "내가 북마크한 청원 조회")
    @GetMapping("/bookmarks")
    public PetitionPageResponse findMyBookmarks(
            @Parameter(hidden = true)
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.findMyBookmarks(userId, page, size);
    }

    @Operation(summary = "내가 작성한 댓글 조회")
    @GetMapping("/comments")
    public UserCommentPageResponse findMyComments(
            @Parameter(hidden = true)
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.findMyComments(userId, page, size);
    }

    @Operation(summary = "내 알림 및 읽음 상태 조회")
    @GetMapping("/notifications")
    public NotificationPageResponse findMyNotifications(
            @Parameter(hidden = true)
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.findMyNotifications(userId, page, size);
    }
}
