package org.skhuconnect.threshold.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;
import org.skhuconnect.global.entity.BaseEntity;
import org.skhuconnect.petition.entity.PetitionCategory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ThresholdSettingTest {

    @Test
    void hasDesignedJpaMapping() throws Exception {
        Table table = ThresholdSetting.class.getAnnotation(Table.class);
        Column category = ThresholdSetting.class.getDeclaredField("category")
                .getAnnotation(Column.class);
        Column thresholdRate = ThresholdSetting.class
                .getDeclaredField("thresholdRate").getAnnotation(Column.class);

        assertThat(ThresholdSetting.class.getSuperclass()).isEqualTo(BaseEntity.class);
        assertThat(table.name()).isEqualTo("threshold_settings");
        assertThat(Arrays.stream(table.indexes()).map(Index::name))
                .containsExactly("ux_threshold_settings_category");
        assertThat(table.indexes()[0].unique()).isTrue();

        assertThat(ThresholdSetting.class.getDeclaredField("id").getAnnotation(Id.class))
                .isNotNull();
        assertThat(ThresholdSetting.class.getDeclaredField("id")
                .getAnnotation(GeneratedValue.class).strategy())
                .isEqualTo(GenerationType.IDENTITY);
        assertThat(category.name()).isEqualTo("category");
        assertThat(category.nullable()).isFalse();
        assertThat(category.length()).isEqualTo(30);
        assertThat(ThresholdSetting.class.getDeclaredField("category")
                .getAnnotation(Enumerated.class).value()).isEqualTo(EnumType.STRING);
        assertThat(ThresholdSetting.class.getDeclaredField("category")
                .getAnnotation(JdbcTypeCode.class).value()).isEqualTo(SqlTypes.VARCHAR);
        assertThat(thresholdRate.precision()).isEqualTo(5);
        assertThat(thresholdRate.scale()).isEqualTo(4);
        assertThat(thresholdRate.nullable()).isFalse();
    }

    @Test
    void calculatesCeilingThresholdAboveMinimum() {
        ThresholdSetting setting = ThresholdSetting.create(
                PetitionCategory.SCHOLARSHIP, 1234, new BigDecimal("0.0100"), 5);

        assertThat(setting.calculateTargetAgreementCount()).isEqualTo(13);
    }

    @Test
    void appliesMinimumWhenCalculatedThresholdIsLower() {
        ThresholdSetting setting = ThresholdSetting.create(
                PetitionCategory.DORMITORY, 100, new BigDecimal("0.0050"), 5);

        assertThat(setting.calculateTargetAgreementCount()).isEqualTo(5);
    }

    @Test
    void exposesFieldsWithoutSetters() throws Exception {
        ThresholdSetting setting = ThresholdSetting.create(
                PetitionCategory.LIBRARY, 2000, new BigDecimal("0.0100"), 5);

        assertThat(setting.getCategory()).isEqualTo(PetitionCategory.LIBRARY);
        assertThat(setting.getTotalStudentCount()).isEqualTo(2000);
        assertThat(setting.getThresholdRate()).isEqualByComparingTo("0.0100");
        assertThat(setting.getMinimumCount()).isEqualTo(5);
        assertThat(Arrays.stream(ThresholdSetting.class.getMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))).isEmpty();
        assertThat(Modifier.isProtected(ThresholdSetting.class
                .getDeclaredConstructor().getModifiers())).isTrue();
    }
}
