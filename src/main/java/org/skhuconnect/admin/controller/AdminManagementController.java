package org.skhuconnect.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.skhuconnect.admin.dto.AdminManagementResponse;
import org.skhuconnect.admin.service.AdminManagementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Management", description = "관리자 소속 및 변경 권한 조회 API")
@RestController
@RequestMapping("/connect/admin/me")
public class AdminManagementController {
    private final AdminManagementService service;

    public AdminManagementController(AdminManagementService service) {
        this.service = service;
    }

    @Operation(summary = "내 관리자 정보 조회", description = "관리자 id, loginId, role을 반환합니다. role은 EMERGENCY_COMMITTEE(비대위) 또는 STUDENT_COUNCIL(학생회)입니다.")
    @GetMapping
    public AdminManagementResponse find(@Parameter(hidden = true) @RequestAttribute("adminId") Long adminId) {
        return service.find(adminId);
    }
}
