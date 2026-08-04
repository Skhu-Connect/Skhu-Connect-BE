package org.skhuconnect.department.dto.response;

import org.skhuconnect.department.entity.Department;

public record DepartmentResponse(
        Long id,
        String code,
        String name
) {

    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getCode(),
                department.getName()
        );
    }
}
