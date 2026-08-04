package org.skhuconnect.department.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.skhuconnect.department.dto.response.DepartmentResponse;
import org.skhuconnect.department.service.DepartmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/connect/departments")
@Tag(name = "Department", description = "학과 조회 API")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @Operation(
            summary = "학과 목록 조회",
            description = "전체 학과 목록을 학과명 오름차순으로 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "학과 목록 조회 성공")
    public List<DepartmentResponse> getDepartments() {
        return departmentService.getDepartments();
    }
}
