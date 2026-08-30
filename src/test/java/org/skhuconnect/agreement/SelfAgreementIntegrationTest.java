package org.skhuconnect.agreement;

import org.junit.jupiter.api.Test;
import org.skhuconnect.agreement.entity.Agreement;
import org.skhuconnect.agreement.repository.AgreementRepository;
import org.skhuconnect.auth.token.dto.TokenIssueResult;
import org.skhuconnect.auth.token.service.UserAuthService;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제로 벌어졌던 시나리오를 그대로 재현한다: 자기 청원에 공감을 눌러 둔 작성자가
 * 그 청원을 영영 삭제할 수 없던 문제(공감 0건이어야 삭제 가능한데 본인 공감도 세었다).
 *
 * 이 제한을 넣기 전에 이미 자기 공감을 눌러 둔 사용자(그랜드파더링 대상)를 재현하기 위해,
 * 서비스가 아니라 리포지토리로 직접 Agreement 행과 agreementCount 를 만든다 - 지금은 서비스
 * 경로로 자기 공감 자체가 막히므로 이렇게 해야 "예전에 이미 생긴" 상태를 흉내낼 수 있다.
 */
@SpringBootTest(properties = {
        "app.mail.from=test@example.com",
        "app.mail.resend-api-key=re_test_key",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.cookie-secure=false"
})
@AutoConfigureMockMvc
@Transactional
class SelfAgreementIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private DepartmentRepository departments;
    @Autowired private PetitionRepository petitions;
    @Autowired private AgreementRepository agreements;
    @Autowired private UserAuthService userAuth;
    @Autowired private PasswordEncoder passwords;

    @Test
    void writerCannotAgreeButCanEscapeAnExistingSelfAgreementToDelete() throws Exception {
        String unique = "self" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Department department = departments.saveAndFlush(
                Department.create("D" + unique, "학과" + unique));
        User writer = users.saveAndFlush(User.create(
                unique + "@office.skhu.ac.kr", unique, passwords.encode("pw-" + unique), department));
        String token = userAuth.login(unique, "pw-" + unique).accessToken();

        Petition petition = petitions.saveAndFlush(Petition.create(
                writer, PetitionCategory.FACILITY, "자기 공감 청원", "본문", 10, LocalDateTime.now()));

        // 1) 새 자기 공감은 막힌다.
        mvc.perform(post("/connect/petitions/" + petition.getId() + "/agreements")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cannot agree to own petition"));
        assertThat(petitions.findById(petition.getId()).orElseThrow().getAgreementCount()).isZero();

        // 2) 그랜드파더링: 규칙을 넣기 전에 이미 생긴 자기 공감을 리포지토리로 직접 흉내낸다.
        agreements.saveAndFlush(Agreement.create(petition, writer));
        petition.addAgreement(LocalDateTime.now());
        petitions.saveAndFlush(petition);

        // 3) 공감 1건이 남아 있는 동안은 삭제가 막힌다 - 화면에서 본 그 상태.
        mvc.perform(delete("/connect/petitions/" + petition.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        // 4) 탈출구: 자기 공감 취소는 계속 허용된다.
        mvc.perform(delete("/connect/petitions/" + petition.getId() + "/agreements")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(petitions.findById(petition.getId()).orElseThrow().getAgreementCount()).isZero();

        // 5) 공감이 0건이 되어야 비로소 삭제된다.
        mvc.perform(delete("/connect/petitions/" + petition.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(petitions.findById(petition.getId()).orElseThrow().isDeleted()).isTrue();
    }
}
