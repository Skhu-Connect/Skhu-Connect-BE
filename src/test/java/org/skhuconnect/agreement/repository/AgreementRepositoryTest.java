package org.skhuconnect.agreement.repository;

import org.junit.jupiter.api.Test;
import org.skhuconnect.agreement.entity.Agreement;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "app.mail.from=test@example.com",
        "app.mail.resend-api-key=re_test_key",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.cookie-secure=false"
})
@Transactional
class AgreementRepositoryTest {

    @Autowired
    private AgreementRepository agreementRepository;
    @Autowired
    private PetitionRepository petitionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void savesFindsAndCountsAgreement() {
        TestData data = saveData();

        Agreement saved = agreementRepository.saveAndFlush(
                Agreement.create(data.petition(), data.user()));

        assertThat(agreementRepository.existsByPetitionIdAndUserId(
                data.petition().getId(), data.user().getId())).isTrue();
        assertThat(agreementRepository.findByPetitionIdAndUserId(
                data.petition().getId(), data.user().getId()))
                .hasValueSatisfying(found ->
                        assertThat(found.getId()).isEqualTo(saved.getId()));
        assertThat(agreementRepository.countByPetitionId(
                data.petition().getId())).isEqualTo(1);
    }

    @Test
    void uniqueConstraintRejectsDuplicateUserAgreement() {
        TestData data = saveData();
        agreementRepository.saveAndFlush(
                Agreement.create(data.petition(), data.user()));

        assertThatThrownBy(() -> agreementRepository.saveAndFlush(
                Agreement.create(data.petition(), data.user())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void locksVisiblePetitionForUpdateAndExcludesDeleted() {
        TestData data = saveData();

        assertThat(petitionRepository.findVisibleByIdForUpdate(
                data.petition().getId())).isPresent();

        data.petition().delete(LocalDateTime.now());
        petitionRepository.flush();

        assertThat(petitionRepository.findVisibleByIdForUpdate(
                data.petition().getId())).isEmpty();
    }

    private TestData saveData() {
        String unique = UUID.randomUUID().toString().replace("-", "");
        Department department = departmentRepository.saveAndFlush(
                Department.create("A" + unique.substring(0, 12),
                        "agreement-" + unique.substring(0, 12)));
        User writer = userRepository.saveAndFlush(User.create(
                unique + "@office.skhu.ac.kr",
                "writer" + unique.substring(0, 12),
                "encoded", department));
        User user = userRepository.saveAndFlush(User.create(
                "u" + unique + "@office.skhu.ac.kr",
                "user" + unique.substring(0, 12),
                "encoded", department));
        Petition petition = petitionRepository.saveAndFlush(Petition.create(
                writer, PetitionCategory.FACILITY,
                "agreement test", "content", 10, LocalDateTime.now()));
        return new TestData(petition, user);
    }

    private record TestData(Petition petition, User user) {
    }
}