package org.skhuconnect.department.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UserDepartmentRequest(
        @Schema(description = "변경할 학과의 고유 ID", example = "1")
        @NotNull @Positive Long departmentId
) {
}
