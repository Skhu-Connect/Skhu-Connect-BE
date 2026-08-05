package org.skhuconnect.petition.repository;

import org.junit.jupiter.api.Test;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.mail.host=localhost",
        "spring.mail.port=2525",
        "spring.mail.username=test",
        "spring.mail.password=test",
        "app.mail.from=test@example.com",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.cookie-secure=false"
})
@Transactional
class PetitionRepositoryTest {

    @Autowired
    private PetitionRepository petitionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void savesAndFindsNonDeletedPetition() {
        User writer = saveWriter();
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 12, 0);

        Petition saved = petitionRepository.saveAndFlush(Petition.create(
                writer,
                PetitionCategory.SCHOLARSHIP,
                "장학금 제도 개선",
                "장학금 제도를 개선해주세요.",
                10,
                now
        ));

        Petition found = petitionRepository.findByIdAndDeletedFalse(saved.getId())
                .orElseThrow();
        assertThat(found.getWriter().getId()).isEqualTo(writer.getId());
        assertThat(found.getTitle()).isEqualTo("장학금 제도 개선");
        assertThat(found.getAgreementDeadline()).isEqualTo(now.plusDays(30));
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void excludesSoftDeletedPetition() {
        Petition petition = petitionRepository.saveAndFlush(Petition.create(
                saveWriter(),
                PetitionCategory.LIBRARY,
                "도서관 개선",
                "도서관을 개선해주세요.",
                5,
                LocalDateTime.now()
        ));

        petition.delete(LocalDateTime.now());
        petitionRepository.flush();

        assertThat(petitionRepository.findByIdAndDeletedFalse(petition.getId()))
                .isEmpty();
        assertThat(petitionRepository.findById(petition.getId()))
                .hasValueSatisfying(found -> assertThat(found.isDeleted()).isTrue());
    }

    private User saveWriter() {
        String unique = UUID.randomUUID().toString().replace("-", "");
        Department department = departmentRepository.saveAndFlush(Department.create(
                "D" + unique.substring(0, 12),
                "테스트학과-" + unique.substring(0, 12)
        ));
        return userRepository.saveAndFlush(User.create(
                unique + "@office.skhu.ac.kr",
                "user" + unique.substring(0, 12),
                "encoded-password",
                department
        ));
    }
}
