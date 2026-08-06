package org.skhuconnect.agreement.service;

import org.junit.jupiter.api.Test;
import org.skhuconnect.agreement.repository.AgreementRepository;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.entity.PetitionStatus;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
class AgreementConcurrencyTest {

    @Autowired
    private AgreementService agreementService;
    @Autowired
    private AgreementRepository agreementRepository;
    @Autowired
    private PetitionRepository petitionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void concurrentAgreementsKeepCountAndRowsConsistentAtThreshold()
            throws Exception {
        String unique = UUID.randomUUID().toString().replace("-", "");
        Department department = departmentRepository.saveAndFlush(
                Department.create("C" + unique.substring(0, 12),
                        "concurrency-" + unique.substring(0, 12)));
        List<User> users = new ArrayList<>();
        Petition petition = null;
        try {
            for (int index = 0; index < 6; index++) {
                users.add(userRepository.saveAndFlush(User.create(
                        index + unique + "@office.skhu.ac.kr",
                        "c" + index + unique.substring(0, 10),
                        "encoded", department)));
            }
            petition = petitionRepository.saveAndFlush(Petition.create(
                    users.get(0), PetitionCategory.FACILITY,
                    "concurrent agreement", "content", 5,
                    LocalDateTime.now()));
            Long petitionId = petition.getId();

            ExecutorService executor = Executors.newFixedThreadPool(6);
            CountDownLatch ready = new CountDownLatch(6);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Boolean>> futures = new ArrayList<>();
            for (User user : users) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        agreementService.agree(user.getId(), petitionId);
                        return true;
                    } catch (RuntimeException exception) {
                        return false;
                    }
                }));
            }
            ready.await();
            start.countDown();
            int successCount = 0;
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    successCount++;
                }
            }
            executor.shutdown();

            Petition found = petitionRepository.findById(petitionId).orElseThrow();
            long rowCount = agreementRepository.countByPetitionId(petitionId);
            assertThat(successCount).isEqualTo(5);
            assertThat(rowCount).isEqualTo(5);
            assertThat(found.getAgreementCount()).isEqualTo(5);
            assertThat(found.getStatus()).isEqualTo(PetitionStatus.UNDER_REVIEW);
            assertThat(found.getReviewStartedAt()).isNotNull();
        } finally {
            if (petition != null && petition.getId() != null) {
                agreementRepository.deleteAllByPetitionId(petition.getId());
                petitionRepository.deleteById(petition.getId());
            }
            userRepository.deleteAllById(
                    users.stream().map(User::getId).toList());
            departmentRepository.deleteById(department.getId());
        }
    }
}