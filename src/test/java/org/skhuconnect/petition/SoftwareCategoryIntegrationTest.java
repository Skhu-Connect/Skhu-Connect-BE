package org.skhuconnect.petition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.auth.token.service.AccessTokenService;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.threshold.entity.ThresholdSetting;
import org.skhuconnect.threshold.repository.ThresholdSettingRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SoftwareCategoryIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private DepartmentRepository departments;
    @Autowired private ThresholdSettingRepository thresholds;
    @Autowired private PetitionRepository petitions;
    @Autowired private AccessTokenService tokens;

    private User user;
    private Department software;
    private Department other;
    private String token;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        software = departments.saveAndFlush(Department.create("SOFTWARE_CONVERGENCE", "소프트웨어융합학부"));
        other = departments.saveAndFlush(Department.create("OTHER" + unique, "다른 학부" + unique));
        user = users.saveAndFlush(User.create(unique + "@office.skhu.ac.kr", unique, "encoded", other));
        token = tokens.issue(user);
        thresholds.saveAndFlush(ThresholdSetting.create(PetitionCategory.SOFTWARE_CONVERGENCE,
                100, new BigDecimal("0.05"), 5));
    }

    @Test
    void categorySelectionAndCreationFollowMyPageDepartmentChangesWithoutLoggingInAgain() throws Exception {
        available(false);
        createSoftware().andExpect(status().isForbidden());
        assertThat(petitions.count()).isZero();

        changeDepartment(software);
        mvc.perform(get("/connect/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.departmentCode").value("SOFTWARE_CONVERGENCE"));
        available(true);
        createSoftware().andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("SOFTWARE_CONVERGENCE"))
                .andExpect(jsonPath("$.targetAgreementCount").value(5));

        changeDepartment(other);
        available(false);
        createSoftware().andExpect(status().isForbidden());
        assertThat(petitions.count()).isEqualTo(1);
    }

    @Test
    void existingCategoriesRemainSelectableForOtherDepartments() throws Exception {
        thresholds.saveAndFlush(ThresholdSetting.create(PetitionCategory.FACILITY, 100, new BigDecimal("0.05"), 5));
        mvc.perform(post("/connect/petitions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"FACILITY\",\"title\":\"title\",\"content\":\"content\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void categorySelectionRequiresUserAuthentication() throws Exception {
        mvc.perform(get("/connect/petitions/categories")).andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerIncludesCategorySelectionAndPermissionResponses() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/connect/petitions/categories'].get").exists())
                .andExpect(jsonPath("$.paths['/connect/admin/me'].get").exists())
                .andExpect(jsonPath("$.paths['/connect/admin/petitions/{petitionId}/hide'].patch.responses['403']").exists())
                .andExpect(content().string(containsString("SOFTWARE_CONVERGENCE")));
    }

    private void changeDepartment(Department department) throws Exception {
        mvc.perform(patch("/connect/users/me/department").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":" + department.getId() + "}"))
                .andExpect(status().isNoContent());
        assertThat(user.getDepartment().getId()).isEqualTo(department.getId());
    }

    private void available(boolean includeSoftware) throws Exception {
        mvc.perform(get("/connect/petitions/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItems("SCHOLARSHIP", "FACILITY", "DORMITORY", "LIBRARY", "DEPARTMENT")))
                .andExpect(jsonPath("$", includeSoftware ? hasItem("SOFTWARE_CONVERGENCE") : not(hasItem("SOFTWARE_CONVERGENCE"))));
    }

    private org.springframework.test.web.servlet.ResultActions createSoftware() throws Exception {
        return mvc.perform(post("/connect/petitions").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"SOFTWARE_CONVERGENCE\",\"title\":\"title\",\"content\":\"content\"}"));
    }
}
