package org.skhuconnect.threshold.repository;

import org.junit.jupiter.api.Test;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.threshold.entity.ThresholdSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.mail.host=localhost",
        "spring.mail.port=2525",
        "spring.mail.username=test",
        "spring.mail.password=test",
        "app.mail.from=test@example.com",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.cookie-secure=false"
})
@Transactional
class ThresholdSettingRepositoryTest {

    @Autowired
    private ThresholdSettingRepository repository;

    @Test
    void savesAndFindsSettingByCategory() {
        ThresholdSetting saved = repository.saveAndFlush(ThresholdSetting.create(
                PetitionCategory.FACILITY, 1234, new BigDecimal("0.0100"), 5));

        ThresholdSetting found = repository.findByCategory(PetitionCategory.FACILITY)
                .orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getCategory()).isEqualTo(PetitionCategory.FACILITY);
        assertThat(found.getTotalStudentCount()).isEqualTo(1234);
        assertThat(found.getThresholdRate()).isEqualByComparingTo("0.0100");
        assertThat(found.getMinimumCount()).isEqualTo(5);
        assertThat(found.calculateTargetAgreementCount()).isEqualTo(13);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void rejectsDuplicateCategory() {
        repository.saveAndFlush(ThresholdSetting.create(
                PetitionCategory.DEPARTMENT, 1000, new BigDecimal("0.0050"), 5));

        assertThatThrownBy(() -> repository.saveAndFlush(ThresholdSetting.create(
                PetitionCategory.DEPARTMENT, 2000, new BigDecimal("0.0050"), 5)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
