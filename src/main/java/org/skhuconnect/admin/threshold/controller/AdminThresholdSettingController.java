package org.skhuconnect.admin.threshold.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.skhuconnect.admin.threshold.dto.AdminThresholdSettingResponse;
import org.skhuconnect.admin.threshold.dto.AdminThresholdSettingUpdateRequest;
import org.skhuconnect.admin.threshold.service.AdminThresholdSettingService;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@ApiResponse(responseCode = "403", description = "담당 카테고리의 관리자만 변경 가능", content = @Content)
@Tag(name = "Admin Threshold Settings", description = "관리자 임계치 설정 API")
@RestController
@RequestMapping("/connect/admin/threshold-settings")
public class AdminThresholdSettingController {

    private final AdminThresholdSettingService service;

    public AdminThresholdSettingController(AdminThresholdSettingService service) {
        this.service = service;
    }

    @Operation(summary = "관리자 임계치 설정 조회")
    @ApiResponse(responseCode = "200", description = "Settings retrieved")
    @GetMapping
    public List<AdminThresholdSettingResponse> findAll() {
        return service.findAll();
    }

    @Operation(summary = "관리자 임계치 설정 수정", description = "SOFTWARE_CONVERGENCE는 최초 PUT 시 설정 행이 없으면 비대위가 전달한 값으로 생성합니다. 이후에는 기존처럼 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Setting updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "404", description = "Setting or administrator not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "동시 최초 생성 충돌. 다시 조회한 후 PUT으로 수정", content = @Content)
    })
    @PutMapping("/{category}")
    public AdminThresholdSettingResponse update(
            @RequestAttribute("adminId") Long adminId,
            @PathVariable PetitionCategory category,
            @Valid @RequestBody AdminThresholdSettingUpdateRequest request
    ) {
        return service.update(adminId, category, request);
    }
}
