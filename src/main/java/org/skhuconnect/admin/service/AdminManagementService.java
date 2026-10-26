package org.skhuconnect.admin.service;

import org.skhuconnect.admin.dto.AdminManagementResponse;
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.repository.AdminRepository;
import org.skhuconnect.admin.token.exception.AdminAuthException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminManagementService {
    private final AdminRepository admins;

    public AdminManagementService(AdminRepository admins) {
        this.admins = admins;
    }

    @Transactional(readOnly = true)
    public AdminManagementResponse find(Long adminId) {
        Admin admin = admins.findById(adminId)
                .orElseThrow(() -> new AdminAuthException(AdminAuthException.Reason.TOKEN_INVALID));
        return new AdminManagementResponse(admin.getId(), admin.getLoginId(), admin.getRole());
    }
}
