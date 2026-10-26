package org.skhuconnect.admin.service;

import org.junit.jupiter.api.Test;
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.exception.AdminManagementException;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminManagementPermissionTest {
    @Test
    void legacyAdministratorKeepsExistingCategoriesAndNoticeManagement() {
        Admin admin = Admin.create("student", "encoded");
        ReflectionTestUtils.setField(admin, "role", null);
        assertThat(admin.getRole()).isEqualTo("STUDENT_COUNCIL");
        for (PetitionCategory category : PetitionCategory.values()) {
            assertThat(AdminManagementPermission.canManage(admin, category))
                    .isEqualTo(category != PetitionCategory.SOFTWARE_CONVERGENCE);
        }
        AdminManagementPermission.requireStudentCouncil(admin);
    }

    @Test
    void softwareAdministratorCannotManageOtherCategoriesOrNotices() {
        Admin admin = Admin.createEmergencyCommittee("software", "encoded");
        for (PetitionCategory category : PetitionCategory.values()) {
            if (category != PetitionCategory.SOFTWARE_CONVERGENCE) {
                assertThatThrownBy(() -> AdminManagementPermission.requireCategory(admin, category))
                        .isInstanceOf(AdminManagementException.class);
            }
        }
        assertThatThrownBy(() -> AdminManagementPermission.requireStudentCouncil(admin))
                .isInstanceOf(AdminManagementException.class);
        AdminManagementPermission.requireCategory(admin, PetitionCategory.SOFTWARE_CONVERGENCE);
    }

    @Test
    void unknownScopeFailsClosed() {
        Admin admin = Admin.create("unknown", "encoded");
        ReflectionTestUtils.setField(admin, "role", "UNRECOGNIZED");
        for (PetitionCategory category : PetitionCategory.values()) {
            assertThat(AdminManagementPermission.canManage(admin, category)).isFalse();
        }
        assertThatThrownBy(() -> AdminManagementPermission.requireStudentCouncil(admin))
                .isInstanceOf(AdminManagementException.class);
    }
}
