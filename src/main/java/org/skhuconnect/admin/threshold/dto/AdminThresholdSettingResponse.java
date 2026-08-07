package org.skhuconnect.admin.threshold.dto;

import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.threshold.entity.ThresholdSetting;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminThresholdSettingResponse(
        PetitionCategory category,
        int totalStudentCount,
        BigDecimal thresholdRate,
        int minimumCount,
        int targetAgreementCount,
        Long updatedByAdminId,
        String changeReason,
        LocalDateTime updatedAt
) {
    public static AdminThresholdSettingResponse from(ThresholdSetting setting) {
        return new AdminThresholdSettingResponse(
                setting.getCategory(),
                setting.getTotalStudentCount(),
                setting.getThresholdRate(),
                setting.getMinimumCount(),
                setting.calculateTargetAgreementCount(),
                setting.getUpdatedByAdmin() == null ? null : setting.getUpdatedByAdmin().getId(),
                setting.getChangeReason(),
                setting.getUpdatedAt()
        );
    }
}