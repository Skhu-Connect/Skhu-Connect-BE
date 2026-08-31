package org.skhuconnect.report;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.repository.AdminRepository;
import org.skhuconnect.admin.token.service.AdminAuthService;
import org.skhuconnect.auth.token.service.UserAuthService;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.threshold.entity.ThresholdSetting;
import org.skhuconnect.threshold.repository.ThresholdSettingRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 작성자가 신고당한 글을 지워도 관리자가 신고를 처리할 수 있어야 한다.
 * 이 경로는 전에 Petition.hide() 의 IllegalStateException 이 그대로 올라와 500 이 났다.
 */
@SpringBootTest(properties = {
        "app.mail.from=test@example.com",
        "app.mail.resend-api-key=re_test_key",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.cookie-secure=false"
})
@AutoConfigureMockMvc
@Transactional
class ReportModerationIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private DepartmentRepository departments;
    @Autowired private ThresholdSettingRepository thresholds;
    @Autowired private AdminRepository admins;
    @Autowired private UserAuthService userAuth;
    @Autowired private AdminAuthService adminAuth;
    @Autowired private PasswordEncoder passwords;

    @Test
    void deletedPetitionKeepsItsReportProcessableAndReadable() throws Exception {
        String writer = userToken("writer");
        String reporter = userToken("reporter");
        String admin = adminToken();
        thresholds.save(ThresholdSetting.create(
                PetitionCategory.FACILITY, 1000, new BigDecimal("0.05"), 10));

        long petitionId = JsonPath.parse(body(post("/connect/petitions").header("Authorization", "Bearer " + writer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"category":"FACILITY","title":"문제가 된 글","content":"신고 사유가 된 원문입니다."}
                        """)))
                .read("$.id", Integer.class);

        long reportId = JsonPath.parse(body(post("/connect/reports").header("Authorization", "Bearer " + reporter)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"petitionId":%d,"reasonType":"ABUSE","reasonDetail":"욕설이 포함된 글이라 신고합니다."}
                        """.formatted(petitionId))))
                .read("$.id", Integer.class);

        mvc.perform(delete("/connect/petitions/" + petitionId)
                        .header("Authorization", "Bearer " + writer))
                .andExpect(status().isNoContent());

        // 삭제됐어도 신고 화면에서는 원문이 보여야 한다.
        mvc.perform(get("/connect/admin/reports").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].targetDeleted").value(true))
                .andExpect(jsonPath("$.content[0].targetTitle").value("문제가 된 글"))
                .andExpect(jsonPath("$.content[0].targetContent").value("신고 사유가 된 원문입니다."));

        // 조치함 처리는 500 이 아니라 정상 종결이어야 하고, 삭제된 글을 숨기지는 않는다.
        mvc.perform(patch("/connect/admin/reports/" + reportId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACTION_TAKEN","processingReason":"욕설 확인","actionType":"HIDE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTION_TAKEN"))
                .andExpect(jsonPath("$.targetHidden").value(false));
    }

    private String body(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse().getContentAsString();
    }

    private String userToken(String prefix) {
        String unique = prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Department department = departments.saveAndFlush(
                Department.create("D" + unique, "학과" + unique));
        users.saveAndFlush(User.create(unique + "@office.skhu.ac.kr", unique,
                passwords.encode("pw-" + unique), department));
        return userAuth.login(unique, "pw-" + unique).accessToken();
    }

    private String adminToken() {
        String unique = "adm" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        admins.saveAndFlush(Admin.create(unique, passwords.encode("pw-" + unique)));
        return adminAuth.login(unique, "pw-" + unique).accessToken();
    }
}
