package org.skhuconnect.admin.threshold.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.repository.AdminRepository;
import org.skhuconnect.admin.threshold.dto.AdminThresholdSettingResponse;
import org.skhuconnect.admin.threshold.dto.AdminThresholdSettingUpdateRequest;
import org.skhuconnect.admin.threshold.exception.AdminThresholdSettingException;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.threshold.entity.ThresholdSetting;
import org.skhuconnect.threshold.repository.ThresholdSettingRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class AdminThresholdSettingServiceTest {

    private ThresholdSettingRepository settings;
    private AdminRepository admins;
    private AdminThresholdSettingService service;
    private ThresholdSetting setting;
    private Admin admin;

    @BeforeEach
    void setUp() {
        settings = mock(ThresholdSettingRepository.class);
        admins = mock(AdminRepository.class);
        service = new AdminThresholdSettingService(settings, admins);
        setting = ThresholdSetting.create(PetitionCategory.FACILITY, 1000,
                new BigDecimal("0.0100"), 5);
        admin = Admin.create("admin", "encoded");
    }

    @Test
    void updateChangesOnlySettingAndRecordsAdminAndReason() {
        when(settings.findByCategory(PetitionCategory.FACILITY)).thenReturn(Optional.of(setting));
        when(admins.findById(7L)).thenReturn(Optional.of(admin));

        AdminThresholdSettingResponse response = service.update(7L,
                PetitionCategory.FACILITY, new AdminThresholdSettingUpdateRequest(
                        1500, new BigDecimal("0.0200"), 10, "annual enrollment update"));

        assertThat(response.totalStudentCount()).isEqualTo(1500);
        assertThat(response.thresholdRate()).isEqualByComparingTo("0.0200");
        assertThat(response.minimumCount()).isEqualTo(10);
        assertThat(response.targetAgreementCount()).isEqualTo(30);
        assertThat(setting.getChangeReason()).isEqualTo("annual enrollment update");
        assertThat(setting.getUpdatedByAdmin()).isSameAs(admin);
    }

    @Test
    void updateRejectsMissingSettingOrAdmin() {
        when(settings.findByCategory(PetitionCategory.FACILITY)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(7L, PetitionCategory.FACILITY,
                new AdminThresholdSettingUpdateRequest(1000,
                        new BigDecimal("0.0100"), 5, "reason")))
                .extracting("reason")
                .isEqualTo(AdminThresholdSettingException.Reason.THRESHOLD_SETTING_NOT_FOUND);

        when(settings.findByCategory(PetitionCategory.FACILITY)).thenReturn(Optional.of(setting));
        when(admins.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(7L, PetitionCategory.FACILITY,
                new AdminThresholdSettingUpdateRequest(1000,
                        new BigDecimal("0.0100"), 5, "reason")))
                .extracting("reason")
                .isEqualTo(AdminThresholdSettingException.Reason.ADMIN_NOT_FOUND);
    }

    @Test
    void concurrentInitialCategoryUniqueViolationIsReportedAsConflict() {
        when(admins.findById(7L)).thenReturn(Optional.of(Admin.createEmergencyCommittee("software", "encoded")));
        org.hibernate.exception.ConstraintViolationException constraint = mock(org.hibernate.exception.ConstraintViolationException.class);
        when(constraint.getConstraintName()).thenReturn("PUBLIC.UX_THRESHOLD_SETTINGS_CATEGORY_INDEX_1");
        when(settings.saveAndFlush(any())).thenThrow(
                new org.springframework.dao.DataIntegrityViolationException("duplicate", constraint));
        assertThatThrownBy(() -> service.update(7L, PetitionCategory.SOFTWARE_CONVERGENCE,
                new AdminThresholdSettingUpdateRequest(100, new BigDecimal("0.05"), 5, "initial")))
                .isInstanceOf(AdminThresholdSettingException.class)
                .extracting("reason")
                .isEqualTo(AdminThresholdSettingException.Reason.THRESHOLD_SETTING_ALREADY_EXISTS);
    }

    @Test
    void schemaCheckViolationIsNotMistakenForConcurrentInitialCreation() {
        when(admins.findById(7L)).thenReturn(Optional.of(Admin.createEmergencyCommittee("software", "encoded")));
        org.hibernate.exception.ConstraintViolationException constraint = mock(org.hibernate.exception.ConstraintViolationException.class);
        when(constraint.getConstraintName()).thenReturn("threshold_settings_chk_1");
        var failure = new org.springframework.dao.DataIntegrityViolationException("check", constraint);
        when(settings.saveAndFlush(any())).thenThrow(failure);
        assertThatThrownBy(() -> service.update(7L, PetitionCategory.SOFTWARE_CONVERGENCE,
                new AdminThresholdSettingUpdateRequest(100, new BigDecimal("0.05"), 5, "initial")))
                .isSameAs(failure);
    }
}
