package org.skhuconnect.threshold.dto;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.threshold.entity.ThresholdSetting;
import java.math.BigDecimal;
public record PublicThresholdSettingResponse(PetitionCategory category,int totalStudentCount,BigDecimal thresholdRate,int minimumCount,int targetAgreementCount){public static PublicThresholdSettingResponse from(ThresholdSetting s){return new PublicThresholdSettingResponse(s.getCategory(),s.getTotalStudentCount(),s.getThresholdRate(),s.getMinimumCount(),s.calculateTargetAgreementCount());}}
