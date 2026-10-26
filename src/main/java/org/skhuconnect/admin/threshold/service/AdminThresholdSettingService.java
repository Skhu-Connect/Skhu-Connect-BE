package org.skhuconnect.admin.threshold.service;

import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.service.AdminManagementPermission;
import org.skhuconnect.admin.repository.AdminRepository;
import org.skhuconnect.admin.notificationlog.service.AdminNotificationLogService;
import org.skhuconnect.admin.notificationlog.entity.*;
import org.skhuconnect.admin.threshold.dto.AdminThresholdSettingResponse;
import org.skhuconnect.admin.threshold.dto.AdminThresholdSettingUpdateRequest;
import org.skhuconnect.admin.threshold.exception.AdminThresholdSettingException;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.threshold.entity.ThresholdSetting;
import org.skhuconnect.threshold.repository.ThresholdSettingRepository;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AdminThresholdSettingService {

    private final ThresholdSettingRepository thresholdSettingRepository;
    private final AdminRepository adminRepository;
    private AdminNotificationLogService notificationLogs;

    public AdminThresholdSettingService(
            ThresholdSettingRepository thresholdSettingRepository,
            AdminRepository adminRepository
    ) {
        this.thresholdSettingRepository = thresholdSettingRepository;
        this.adminRepository = adminRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminThresholdSettingResponse> findAll() {
        return thresholdSettingRepository.findAll(Sort.by("category")).stream()
                .map(AdminThresholdSettingResponse::from)
                .toList();
    }

    @Transactional
    public AdminThresholdSettingResponse update(
            Long adminId,
            PetitionCategory category,
            AdminThresholdSettingUpdateRequest request
    ) {
        ThresholdSetting setting = thresholdSettingRepository.findByCategory(category).orElse(null);
        if (setting == null && category != PetitionCategory.SOFTWARE_CONVERGENCE) {
            throw error(AdminThresholdSettingException.Reason.THRESHOLD_SETTING_NOT_FOUND);
        }
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> error(AdminThresholdSettingException.Reason.ADMIN_NOT_FOUND));
        AdminManagementPermission.requireCategory(admin, category);
        boolean creating = setting == null;
        if (creating) {
            setting = ThresholdSetting.create(category, request.totalStudentCount(),
                    request.thresholdRate(), request.minimumCount());
        }
        setting.update(request.totalStudentCount(), request.thresholdRate(),
                request.minimumCount(), request.changeReason(), admin);
        if (creating) {
            try {
                setting = thresholdSettingRepository.saveAndFlush(setting);
            } catch (DataIntegrityViolationException exception) {
                // 카테고리 UNIQUE 충돌만 409로 구분한다. CHECK 등 다른 스키마 오류는 숨기지 않는다.
                if (isCategoryDuplicate(exception)) {
                    throw error(AdminThresholdSettingException.Reason.THRESHOLD_SETTING_ALREADY_EXISTS);
                }
                throw exception;
            }
        }
        if (notificationLogs != null) notificationLogs.record(NotificationLogType.THRESHOLD_SETTING_UPDATED, admin, NotificationLogTargetType.THRESHOLD_SETTING, setting.getId(), "Threshold setting updated: " + category);
        return AdminThresholdSettingResponse.from(setting);
    }

    private boolean isCategoryDuplicate(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && violation.getConstraintName() != null
                    && violation.getConstraintName().toLowerCase(Locale.ROOT)
                            .contains("ux_threshold_settings_category")) {
                return true;
            }
        }
        return false;
    }

    @org.springframework.beans.factory.annotation.Autowired
    void setNotificationLogs(AdminNotificationLogService notificationLogs) { this.notificationLogs = notificationLogs; }

    private AdminThresholdSettingException error(
            AdminThresholdSettingException.Reason reason) {
        return new AdminThresholdSettingException(reason);
    }
}
