package org.skhuconnect.user.block.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.skhuconnect.user.block.dto.UserBlockRequest;
import org.skhuconnect.user.block.dto.UserBlockResponse;
import org.skhuconnect.user.block.service.UserBlockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Block", description = "사용자 영구 차단 API")
@RestController
@RequestMapping("/connect/users/me/blocks")
public class UserBlockController {
    private final UserBlockService userBlockService;
    public UserBlockController(UserBlockService userBlockService) { this.userBlockService = userBlockService; }

    @Operation(summary = "콘텐츠 작성자 영구 차단", description = "청원 또는 댓글·대댓글 ID로 작성자를 영구 차단합니다. 차단은 단방향이며 해제할 수 없고, 차단 대상에게는 알리지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "영구 차단 성공"),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류 또는 본인 차단", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "404", description = "콘텐츠 또는 작성자를 찾을 수 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 차단한 사용자", content = @Content)
    })
    @PostMapping
    public ResponseEntity<UserBlockResponse> block(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "targetType은 PETITION(해당 청원 작성자 차단) 또는 COMMENT(해당 댓글·대댓글 작성자 차단)입니다.",
                    content = @Content(examples = {
                            @ExampleObject(name = "청원 작성자 차단", value = "{\"targetType\":\"PETITION\",\"contentId\":42}"),
                            @ExampleObject(name = "댓글·대댓글 작성자 차단", value = "{\"targetType\":\"COMMENT\",\"contentId\":42}")
                    })
            )
            @Valid @RequestBody UserBlockRequest request) {
        return ResponseEntity.status(201).body(userBlockService.block(userId, request));
    }
}
