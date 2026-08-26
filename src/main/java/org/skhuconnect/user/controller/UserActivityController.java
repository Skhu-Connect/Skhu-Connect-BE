package org.skhuconnect.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.skhuconnect.notification.dto.NotificationPageResponse;
import org.skhuconnect.petition.dto.response.PetitionPageResponse;
import org.skhuconnect.user.dto.UserCommentPageResponse;
import org.skhuconnect.user.dto.UserMeResponse;
import org.skhuconnect.user.dto.NotificationSettingsResponse;
import org.skhuconnect.user.dto.NotificationSettingsUpdateRequest;
import org.skhuconnect.auth.loginid.dto.LoginIdResponse;
import org.skhuconnect.user.dto.LoginIdUpdateRequest;
import org.skhuconnect.user.dto.PasswordChangeRequest;
import org.skhuconnect.user.service.UserActivityService;
import org.skhuconnect.user.service.UserAccountService;
import org.skhuconnect.user.dto.UserWithdrawalRequest;
import org.skhuconnect.user.service.UserWithdrawalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Activity", description = "로그인 사용자 본인 정보 및 활동 내역 API")
@RestController
@RequestMapping("/connect/users/me")
public class UserActivityController {

    private final UserActivityService service;
    private final UserWithdrawalService withdrawalService;
    private final UserAccountService accountService;

    public UserActivityController(
            UserActivityService service,
            UserWithdrawalService withdrawalService,
            UserAccountService accountService
    ) {
        this.service = service;
        this.withdrawalService = withdrawalService;
        this.accountService = accountService;
    }

    @Operation(
            summary = "내 정보 조회",
            description = "기본 정보와 전체 알림 수신 여부, 종류별 notificationSettings를 반환합니다."
    )
    @GetMapping
    public UserMeResponse findMe(
            @Parameter(hidden = true)
            @RequestAttribute("userId") Long userId
    ) {
        return service.findMe(userId);
    }

    @Operation(
            summary = "알림 종류별 수신 설정 변경",
            description = "agreement, answer, reply, like, notice 중 보낸 항목만 변경합니다. 생략하거나 null인 항목은 유지하며, 응답은 변경 후 전체 설정입니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "알림 설정 변경 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(
                            implementation = NotificationSettingsResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "변경할 설정 없음", content = @Content),
            @ApiResponse(responseCode = "401", description = "Access Token 무효", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content)
    })
    @PatchMapping("/notification-settings")
    public NotificationSettingsResponse updateNotificationSettings(
            @Parameter(hidden = true)
            @RequestAttribute("userId") Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "5개 항목 중 하나 이상을 전달합니다. 생략하거나 null인 항목은 변경하지 않습니다.",
                    content = @Content(
                            schema = @Schema(
                                    implementation = NotificationSettingsUpdateRequest.class),
                            examples = @ExampleObject(
                                    name = "댓글 공감 알림 끄기",
                                    value = "{\"like\":false}"
                            )
                    )
            )
            @RequestBody NotificationSettingsUpdateRequest request
    ) {
        return service.updateNotificationSettings(userId, request);
    }

    @Operation(
            summary = "로그인 아이디 변경",
            description = "현재 비밀번호를 확인한 뒤 로그인 아이디를 변경합니다. 기존 로그인 세션과 토큰은 유지됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "아이디 변경 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginIdResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류 또는 현재 아이디와 동일", content = @Content),
            @ApiResponse(responseCode = "401", description = "Access Token 오류 또는 현재 비밀번호 불일치", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 아이디", content = @Content)
    })
    @PatchMapping("/login-id")
    public LoginIdResponse changeLoginId(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(
                            value = "{\"newLoginId\":\"new-login-id\",\"password\":\"current-password\"}")))
            @Valid @RequestBody LoginIdUpdateRequest request
    ) {
        return accountService.changeLoginId(userId, request);
    }

    @Operation(
            summary = "로그인 상태 비밀번호 변경",
            description = "현재 비밀번호를 확인하고 새 비밀번호를 저장합니다. 기존 로그인 세션과 토큰은 유지됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류 또는 기존 비밀번호와 동일", content = @Content),
            @ApiResponse(responseCode = "401", description = "Access Token 오류 또는 현재 비밀번호 불일치", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content)
    })
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(
                            value = "{\"currentPassword\":\"current-password\",\"newPassword\":\"new-password\"}")))
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        accountService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
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
