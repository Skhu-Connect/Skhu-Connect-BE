package org.skhuconnect.admin.threshold.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdminThresholdSettingUpdateRequest(
        @Min(1) int totalStudentCount,
        @NotNull @DecimalMin("0.0001") @DecimalMax("1.0000") BigDecimal thresholdRate,
        @Min(1) int minimumCount,
        @NotBlank @Size(max = 500) String changeReason
) {
}