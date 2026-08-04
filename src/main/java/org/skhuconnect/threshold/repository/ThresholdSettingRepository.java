package org.skhuconnect.threshold.repository;

import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.threshold.entity.ThresholdSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ThresholdSettingRepository
        extends JpaRepository<ThresholdSetting, Long> {

    Optional<ThresholdSetting> findByCategory(PetitionCategory category);
}
