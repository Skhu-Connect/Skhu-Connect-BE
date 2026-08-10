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

@Tag(name = "愿由ъ옄 ?꾧퀎移??ㅼ젙s", description = "Administrator threshold setting API")
@RestController
@RequestMapping("/connect/admin/threshold-settings")
public class AdminThresholdSettingController {

    private final AdminThresholdSettingService service;

    public AdminThresholdSettingController(AdminThresholdSettingService service) {
        this.service = service;
    }

    @Operation(summary = "List threshold settings")
    @ApiResponse(responseCode = "200", description = "Settings retrieved")
    @GetMapping
    public List<AdminThresholdSettingResponse> findAll() {
        return service.findAll();
    }

    @Operation(summary = "Update threshold setting")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Setting updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "404", description = "Setting or administrator not found", content = @Content)
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