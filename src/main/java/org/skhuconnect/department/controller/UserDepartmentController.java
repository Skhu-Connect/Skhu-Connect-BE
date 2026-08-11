package org.skhuconnect.department.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.skhuconnect.department.dto.request.UserDepartmentRequest;
import org.skhuconnect.department.service.UserDepartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/connect/users/me/department")
@Tag(name = "Department", description = "학과 조회 및 사용자 소속 학과 관리 API")
public class UserDepartmentController {
    private final UserDepartmentService userDepartmentService;

    public UserDepartmentController(UserDepartmentService userDepartmentService) {
        this.userDepartmentService = userDepartmentService;
    }

    @PatchMapping
    @Operation(
            summary = "마이페이지 소속 학과 수정 API",
            description = "로그인이 필요합니다. 요청한 departmentId에 해당하는 학과로 현재 사용자의 소속 학과만 변경합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "소속 학과 수정 성공"),
            @ApiResponse(responseCode = "400", description = "departmentId 누락 또는 잘못된 값", content = @Content),
            @ApiResponse(responseCode = "401", description = "로그인 필요 또는 유효하지 않은 Access Token", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 또는 학과를 찾을 수 없음", content = @Content)
    })
    public ResponseEntity<Void> updateDepartment(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody UserDepartmentRequest request
    ) {
        userDepartmentService.updateDepartment(userId, request.departmentId());
        return ResponseEntity.noContent().build();
    }
}
