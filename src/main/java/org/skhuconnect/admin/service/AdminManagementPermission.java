package org.skhuconnect.admin.service;

import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.exception.AdminManagementException;
import org.skhuconnect.petition.entity.PetitionCategory;

public final class AdminManagementPermission {

    private AdminManagementPermission() {
    }

    public static boolean canManage(Admin admin, PetitionCategory category) {
        if (admin == null || category == null) return false;
        boolean software = category == PetitionCategory.SOFTWARE_CONVERGENCE;
        if ("STUDENT_COUNCIL".equals(admin.getRole())) return !software;
        if ("EMERGENCY_COMMITTEE".equals(admin.getRole())) return software;
        return false;
    }

    public static void requireCategory(Admin admin, PetitionCategory category) {
        if (!canManage(admin, category)) throw forbidden();
    }

    public static void requireStudentCouncil(Admin admin) {
        if (admin == null || !"STUDENT_COUNCIL".equals(admin.getRole())) {
            throw forbidden();
        }
    }

    private static AdminManagementException forbidden() {
        return new AdminManagementException();
    }
}
