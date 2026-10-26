package org.skhuconnect.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.repository.AdminRepository;
import org.skhuconnect.auth.token.service.AccessTokenService;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.comment.entity.PetitionAnonymousNumber;
import org.skhuconnect.comment.repository.CommentRepository;
import org.skhuconnect.comment.repository.PetitionAnonymousNumberRepository;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.notice.entity.Notice;
import org.skhuconnect.notice.repository.NoticeRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.entity.PetitionStatus;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.report.entity.Report;
import org.skhuconnect.report.entity.ReportReasonType;
import org.skhuconnect.report.entity.ReportStatus;
import org.skhuconnect.report.repository.ReportRepository;
import org.skhuconnect.threshold.entity.ThresholdSetting;
import org.skhuconnect.threshold.repository.ThresholdSettingRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminManagementIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private AdminRepository admins;
    @Autowired private DepartmentRepository departments;
    @Autowired private UserRepository users;
    @Autowired private PetitionRepository petitions;
    @Autowired private CommentRepository comments;
    @Autowired private PetitionAnonymousNumberRepository anonymousNumbers;
    @Autowired private ThresholdSettingRepository thresholds;
    @Autowired private ReportRepository reports;
    @Autowired private NoticeRepository notices;
    @Autowired private AccessTokenService tokens;

    private String studentToken;
    private String softwareToken;
    private User writer;
    private Admin studentAdmin;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        studentAdmin = admins.saveAndFlush(Admin.create("student" + unique, "encoded"));
        studentToken = tokens.issue(studentAdmin);
        softwareToken = tokens.issue(admins.saveAndFlush(
                Admin.createEmergencyCommittee("software" + unique, "encoded")));
        Department department = departments.saveAndFlush(Department.create("D" + unique, "테스트" + unique));
        writer = users.saveAndFlush(User.create(unique + "@office.skhu.ac.kr", unique, "encoded", department));
    }

    @ParameterizedTest
    @CsvSource({"FACILITY,false", "SOFTWARE_CONVERGENCE,true"})
    void bothAdministratorsSeeAllPetitionsButOnlyResponsibleAdministratorCanModerate(
            PetitionCategory category, boolean software) throws Exception {
        Petition petition = petition(category);
        Petition other = petition(software ? PetitionCategory.FACILITY : PetitionCategory.SOFTWARE_CONVERGENCE);
        Comment comment = comment(petition);
        String allowed = software ? softwareToken : studentToken;
        String denied = software ? studentToken : softwareToken;
        for (String token : new String[]{allowed, denied}) {
            call("GET", "/connect/admin/petitions", token, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id", hasItems(petition.getId().intValue(), other.getId().intValue())));
            call("GET", path(petition) + "/comments", token, null).andExpect(status().isOk());
        }
        call("PATCH", path(petition) + "/hide", denied, "{\"hiddenReason\":\"test\"}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("ADMIN_FORBIDDEN"));
        assertThat(petition.isHidden()).isFalse();
        call("PATCH", path(petition) + "/hide", allowed, "{\"hiddenReason\":\"test\"}")
                .andExpect(status().isOk());
        call("PATCH", path(petition) + "/restore", denied, null).andExpect(status().isForbidden());
        assertThat(petition.isHidden()).isTrue();
        call("PATCH", path(petition) + "/restore", allowed, null).andExpect(status().isOk());
        String commentPath = path(petition) + "/comments/" + comment.getId();
        call("PATCH", commentPath + "/hide", denied, "{\"hiddenReason\":\"test\"}")
                .andExpect(status().isForbidden());
        assertThat(comment.isHidden()).isFalse();
        call("PATCH", commentPath + "/hide", allowed, "{\"hiddenReason\":\"test\"}")
                .andExpect(status().isOk());
        call("PATCH", commentPath + "/restore", denied, null).andExpect(status().isForbidden());
        assertThat(comment.isHidden()).isTrue();
        call("PATCH", commentPath + "/restore", allowed, null).andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({"FACILITY,false", "SOFTWARE_CONVERGENCE,true"})
    void answerRegistrationAndUpdateRequireCategoryPermission(PetitionCategory category, boolean software) throws Exception {
        Petition petition = petition(category);
        String allowed = software ? softwareToken : studentToken;
        String denied = software ? studentToken : softwareToken;
        String body = "{\"content\":\"answer\",\"answerSource\":\"OPERATION_TEAM\"}";
        call("POST", path(petition) + "/answer", denied, body).andExpect(status().isForbidden());
        assertThat(petition.getStatus()).isEqualTo(PetitionStatus.UNDER_REVIEW);
        call("POST", path(petition) + "/answer", allowed, body).andExpect(status().isOk());
        assertThat(petition.getStatus()).isEqualTo(PetitionStatus.ANSWERED);
        call("PUT", path(petition) + "/answer", denied, body).andExpect(status().isForbidden());
        call("PUT", path(petition) + "/answer", allowed, body).andExpect(status().isOk());
        call("GET", path(petition) + "/answer", denied, null).andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({"FACILITY,false", "SOFTWARE_CONVERGENCE,true"})
    void thresholdPermissionCoversStudentCountAndRateWithoutChangingExistingPetition(
            PetitionCategory category, boolean software) throws Exception {
        Petition petition = petition(category);
        ThresholdSetting setting = thresholds.saveAndFlush(ThresholdSetting.create(category, 100, new BigDecimal("0.01"), 5));
        String allowed = software ? softwareToken : studentToken;
        String denied = software ? studentToken : softwareToken;
        String body = "{\"totalStudentCount\":200,\"thresholdRate\":0.05,\"minimumCount\":5,\"changeReason\":\"test\"}";
        call("PUT", "/connect/admin/threshold-settings/" + category, denied, body).andExpect(status().isForbidden());
        assertThat(setting.getTotalStudentCount()).isEqualTo(100);
        call("PUT", "/connect/admin/threshold-settings/" + category, allowed, body)
                .andExpect(status().isOk()).andExpect(jsonPath("$.targetAgreementCount").value(10));
        assertThat(petition.getTargetAgreementCount()).isEqualTo(1);
    }

    @Test
    void softwareAdministratorCannotPerformAnyNoticeMutation() throws Exception {
        Notice notice = notices.saveAndFlush(Notice.create(studentAdmin, "title", "content"));
        String body = "{\"title\":\"changed\",\"content\":\"changed\"}";
        call("POST", "/connect/admin/notices", softwareToken, body).andExpect(status().isForbidden());
        call("PUT", "/connect/admin/notices/" + notice.getId(), softwareToken, body).andExpect(status().isForbidden());
        for (String action : new String[]{"publish", "hide", "republish"}) {
            call("PATCH", "/connect/admin/notices/" + notice.getId() + "/" + action, softwareToken, null)
                    .andExpect(status().isForbidden());
        }
        assertThat(notice.getTitle()).isEqualTo("title");
        assertThat(notice.getStatus().name()).isEqualTo("DRAFT");
        call("PUT", "/connect/admin/notices/" + notice.getId(), studentToken, body).andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({"FACILITY,false", "SOFTWARE_CONVERGENCE,true"})
    void reportProcessingUsesTargetCategoryAndCannotBeUsedToBypassModerationScope(
            PetitionCategory category, boolean software) throws Exception {
        Petition petition = petition(category);
        Report report = reports.saveAndFlush(Report.forPetition(writer, petition, ReportReasonType.SPAM, "test report detail"));
        String allowed = software ? softwareToken : studentToken;
        String denied = software ? studentToken : softwareToken;
        String body = "{\"status\":\"ACTION_TAKEN\",\"actionType\":\"HIDE\",\"processingReason\":\"test\"}";
        call("PATCH", "/connect/admin/reports/" + report.getId(), denied, body).andExpect(status().isForbidden());
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(petition.isHidden()).isFalse();
        call("PATCH", "/connect/admin/reports/" + report.getId(), allowed, body).andExpect(status().isOk());
        assertThat(petition.isHidden()).isTrue();
    }

    @Test
    void emergencyCommitteeCanBanUserThroughOwnCategoryReport() throws Exception {
        Petition petition = petition(PetitionCategory.SOFTWARE_CONVERGENCE);
        Report report = reports.saveAndFlush(Report.forPetition(writer, petition, ReportReasonType.SPAM, "test report detail"));
        call("PATCH", "/connect/admin/reports/" + report.getId(), softwareToken,
                "{\"status\":\"ACTION_TAKEN\",\"actionType\":\"USER_LOGIN_BAN\",\"processingReason\":\"test\"}")
                .andExpect(status().isOk());
        assertThat(report.getStatus()).isEqualTo(ReportStatus.ACTION_TAKEN);
        assertThat(writer.isLoginBanned()).isTrue();
    }

    @Test
    void identityEndpointExposesDatabasePermissionsUsingExistingAdminToken() throws Exception {
        call("GET", "/connect/admin/me", softwareToken, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EMERGENCY_COMMITTEE"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.loginId").isString());
        call("GET", "/connect/admin/me", studentToken, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentAdmin.getId().intValue()))
                .andExpect(jsonPath("$.loginId").value(studentAdmin.getLoginId()))
                .andExpect(jsonPath("$.role").value("STUDENT_COUNCIL"));
    }

    @Test
    void commentReportReturnsParentPetitionCategoryAndUsesItForAuthorization() throws Exception {
        Petition petition = petition(PetitionCategory.SOFTWARE_CONVERGENCE);
        Comment comment = comment(petition);
        Report report = reports.saveAndFlush(Report.forComment(writer, comment, ReportReasonType.SPAM, "test report detail"));
        call("GET", "/connect/admin/reports", studentToken, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].petitionCategory").value("SOFTWARE_CONVERGENCE"));
        String body = "{\"status\":\"ACTION_TAKEN\",\"actionType\":\"HIDE\",\"processingReason\":\"test\"}";
        call("PATCH", "/connect/admin/reports/" + report.getId(), studentToken, body)
                .andExpect(status().isForbidden());
        assertThat(comment.isHidden()).isFalse();
        call("PATCH", "/connect/admin/reports/" + report.getId(), softwareToken, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.petitionCategory").value("SOFTWARE_CONVERGENCE"));
        assertThat(comment.isHidden()).isTrue();
        assertThat(petition.isHidden()).isFalse();
    }

    @Test
    void emergencyCommitteeFirstPutCreatesMissingSoftwareThresholdAndNextPutUpdatesIt() throws Exception {
        String path = "/connect/admin/threshold-settings/SOFTWARE_CONVERGENCE";
        String body = "{\"totalStudentCount\":200,\"thresholdRate\":0.05,\"minimumCount\":5,\"changeReason\":\"initial setup\"}";
        call("PUT", path, studentToken, body).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("ADMIN_FORBIDDEN"));
        assertThat(thresholds.findByCategory(PetitionCategory.SOFTWARE_CONVERGENCE)).isEmpty();
        call("PUT", path, softwareToken, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.targetAgreementCount").value(10))
                .andExpect(jsonPath("$.changeReason").value("initial setup"))
                .andExpect(jsonPath("$.updatedByAdminId").isNumber());
        Long id = thresholds.findByCategory(PetitionCategory.SOFTWARE_CONVERGENCE).orElseThrow().getId();
        call("PUT", path, softwareToken,
                "{\"totalStudentCount\":300,\"thresholdRate\":0.05,\"minimumCount\":5,\"changeReason\":\"update\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.targetAgreementCount").value(15));
        assertThat(thresholds.findByCategory(PetitionCategory.SOFTWARE_CONVERGENCE).orElseThrow().getId())
                .isEqualTo(id);
        assertThat(thresholds.count()).isEqualTo(1);
    }

    private Petition petition(PetitionCategory category) {
        Petition petition = Petition.create(writer, category, "title", "content", 1, LocalDateTime.now());
        petition.addAgreement(LocalDateTime.now());
        return petitions.saveAndFlush(petition);
    }

    private Comment comment(Petition petition) {
        PetitionAnonymousNumber number = anonymousNumbers.saveAndFlush(PetitionAnonymousNumber.create(petition, writer, 1));
        return comments.saveAndFlush(Comment.create(petition, writer, number, "comment"));
    }

    private String path(Petition petition) { return "/connect/admin/petitions/" + petition.getId(); }

    private ResultActions call(String method, String path, String token, String body) throws Exception {
        var builder = request(HttpMethod.valueOf(method), path).header("Authorization", "Bearer " + token);
        if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(builder);
    }
}
