package org.skhuconnect.comment.service;

import org.junit.jupiter.api.Test;
import org.skhuconnect.comment.dto.request.CommentCreateRequest;
import org.skhuconnect.comment.repository.CommentLikeRepository;
import org.skhuconnect.comment.repository.CommentRepository;
import org.skhuconnect.comment.repository.PetitionAnonymousNumberRepository;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.mail.host=localhost", "spring.mail.port=2525",
        "spring.mail.username=test", "spring.mail.password=test",
        "app.mail.from=test@example.com",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.cookie-secure=false"
})
class CommentConcurrencyTest {

    @Autowired private CommentService commentService;
    @Autowired private CommentRepository commentRepository;
    @Autowired private PetitionAnonymousNumberRepository mappingRepository;
    @Autowired private CommentLikeRepository likeRepository;
    @Autowired private PetitionRepository petitionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DepartmentRepository departmentRepository;

    @Test
    void logicalDeleteThenRewriteReusesPermanentAnonymousNumber() {
        String unique = UUID.randomUUID().toString().replace("-", "");
        Department department = departmentRepository.saveAndFlush(Department.create(
                "R" + unique.substring(0, 12), "comment-reuse-" + unique.substring(0, 8)));
        User writer = userRepository.saveAndFlush(User.create(
                "w" + unique + "@office.skhu.ac.kr", "rw" + unique.substring(0, 10),
                "encoded", department));
        User user = userRepository.saveAndFlush(User.create(
                "u" + unique + "@office.skhu.ac.kr", "ru" + unique.substring(0, 10),
                "encoded", department));
        Petition petition = petitionRepository.saveAndFlush(Petition.create(
                writer, PetitionCategory.FACILITY, "reuse", "content", 10,
                LocalDateTime.now()));
        try {
            var first = commentService.create(user.getId(), petition.getId(),
                    new CommentCreateRequest("first"));
            commentService.delete(user.getId(), petition.getId(), first.id());
            var second = commentService.create(user.getId(), petition.getId(),
                    new CommentCreateRequest("second"));

            assertThat(second.anonymousNumber()).isEqualTo(first.anonymousNumber());
            assertThat(mappingRepository.findByPetitionIdAndUserId(
                    petition.getId(), user.getId()))
                    .hasValueSatisfying(mapping -> assertThat(
                            mapping.getAnonymousNumber()).isEqualTo(first.anonymousNumber()));
        } finally {
            likeRepository.deleteAllByCommentPetitionId(petition.getId());
            commentRepository.deleteAllByPetitionId(petition.getId());
            mappingRepository.deleteAllByPetitionId(petition.getId());
            petitionRepository.deleteById(petition.getId());
            userRepository.deleteAllById(List.of(writer.getId(), user.getId()));
            departmentRepository.deleteById(department.getId());
        }
    }
    @Test
    void concurrentFirstCommentsReceiveUniqueSequentialNumbers() throws Exception {
        String unique = UUID.randomUUID().toString().replace("-", "");
        Department department = departmentRepository.saveAndFlush(Department.create(
                "N" + unique.substring(0, 12), "comment-concurrent-" + unique.substring(0, 8)));
        List<User> users = new ArrayList<>();
        Petition petition = null;
        try {
            for (int index = 0; index < 5; index++) {
                users.add(userRepository.saveAndFlush(User.create(
                        index + unique + "@office.skhu.ac.kr",
                        "n" + index + unique.substring(0, 10), "encoded", department)));
            }
            petition = petitionRepository.saveAndFlush(Petition.create(
                    users.get(0), PetitionCategory.FACILITY,
                    "concurrent comments", "content", 10, LocalDateTime.now()));
            Long petitionId = petition.getId();
            ExecutorService executor = Executors.newFixedThreadPool(5);
            CountDownLatch ready = new CountDownLatch(5);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Integer>> futures = new ArrayList<>();
            for (User user : users) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return commentService.create(user.getId(), petitionId,
                            new CommentCreateRequest("content")).anonymousNumber();
                }));
            }
            ready.await();
            start.countDown();
            List<Integer> numbers = new ArrayList<>();
            for (Future<Integer> future : futures) {
                numbers.add(future.get());
            }
            executor.shutdown();

            assertThat(numbers).containsExactlyInAnyOrder(1, 2, 3, 4, 5);
            assertThat(new HashSet<>(numbers)).hasSize(5);
            assertThat(commentRepository.findByPetitionIdAndDeletedFalse(
                    petitionId, org.springframework.data.domain.Pageable.unpaged()))
                    .hasSize(5);
        } finally {
            if (petition != null) {
                likeRepository.deleteAllByCommentPetitionId(petition.getId());
                commentRepository.deleteAllByPetitionId(petition.getId());
                mappingRepository.deleteAllByPetitionId(petition.getId());
                petitionRepository.deleteById(petition.getId());
            }
            userRepository.deleteAllById(users.stream().map(User::getId).toList());
            departmentRepository.deleteById(department.getId());
        }
    }
}
