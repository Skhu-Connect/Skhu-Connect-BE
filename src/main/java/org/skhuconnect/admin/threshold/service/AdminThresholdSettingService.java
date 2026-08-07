package org.skhuconnect.admin.threshold.service;

import org.skhuconnect.admin.entity.Admin;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        ThresholdSetting setting = thresholdSettingRepository.findByCategory(category)
                .orElseThrow(() -> error(
                        AdminThresholdSettingException.Reason.THRESHOLD_SETTING_NOT_FOUND));
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> error(AdminThresholdSettingException.Reason.ADMIN_NOT_FOUND));
        setting.update(request.totalStudentCount(), request.thresholdRate(),
                request.minimumCount(), request.changeReason(), admin);
        if (notificationLogs != null) notificationLogs.record(NotificationLogType.THRESHOLD_SETTING_UPDATED, admin, NotificationLogTargetType.THRESHOLD_SETTING, setting.getId(), "Threshold setting updated: " + category);
        return AdminThresholdSettingResponse.from(setting);
    }

    @org.springframework.beans.factory.annotation.Autowired
    void setNotificationLogs(AdminNotificationLogService notificationLogs) { this.notificationLogs = notificationLogs; }

    private AdminThresholdSettingException error(
            AdminThresholdSettingException.Reason reason) {
        return new AdminThresholdSettingException(reason);
    }
}