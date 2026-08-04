package org.skhuconnect.threshold.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.skhuconnect.global.entity.BaseEntity;
import org.skhuconnect.petition.entity.PetitionCategory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Entity
@Table(name = "threshold_settings", indexes = {
        @Index(name = "ux_threshold_settings_category",
                columnList = "category", unique = true)
})
public class ThresholdSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "category", nullable = false, length = 30)
    private PetitionCategory category;

    @Column(name = "total_student_count", nullable = false)
    private int totalStudentCount;

    @Column(name = "threshold_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal thresholdRate;

    @Column(name = "minimum_count", nullable = false)
    private int minimumCount;

    protected ThresholdSetting() {
    }

    private ThresholdSetting(
            PetitionCategory category,
            int totalStudentCount,
            BigDecimal thresholdRate,
            int minimumCount
    ) {
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.totalStudentCount = totalStudentCount;
        this.thresholdRate = Objects.requireNonNull(
                thresholdRate, "thresholdRate must not be null");
        this.minimumCount = minimumCount;
    }

    public static ThresholdSetting create(
            PetitionCategory category,
            int totalStudentCount,
            BigDecimal thresholdRate,
            int minimumCount
    ) {
        return new ThresholdSetting(
                category, totalStudentCount, thresholdRate, minimumCount);
    }

    public int calculateTargetAgreementCount() {
        int calculatedCount = thresholdRate
                .multiply(BigDecimal.valueOf(totalStudentCount))
                .setScale(0, RoundingMode.CEILING)
                .intValueExact();
        return Math.max(calculatedCount, minimumCount);
    }

    public Long getId() {
        return id;
    }

    public PetitionCategory getCategory() {
        return category;
    }

    public int getTotalStudentCount() {
        return totalStudentCount;
    }

    public BigDecimal getThresholdRate() {
        return thresholdRate;
    }

    public int getMinimumCount() {
        return minimumCount;
    }
}
