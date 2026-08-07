package org.skhuconnect.bookmark.service;

import org.junit.jupiter.api.Test;
import org.skhuconnect.bookmark.exception.BookmarkException;
import org.skhuconnect.bookmark.repository.BookmarkRepository;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.mail.from=test@example.com",
        "app.mail.resend-api-key=re_test_key",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.cookie-secure=false"
})
class BookmarkConcurrencyTest {

    @Autowired private BookmarkService bookmarkService;
    @Autowired private BookmarkRepository bookmarkRepository;
    @Autowired private PetitionRepository petitionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DepartmentRepository departmentRepository;

    @Test
    void concurrentDuplicateRequestsCreateExactlyOneBookmark() throws Exception {
        String unique = UUID.randomUUID().toString().replace("-", "");
        Department department = departmentRepository.saveAndFlush(Department.create(
                "K" + unique.substring(0, 12), "bookmark-concurrent-" + unique.substring(0, 8)));
        User writer = userRepository.saveAndFlush(User.create(
                "w" + unique + "@office.skhu.ac.kr", "w" + unique.substring(0, 12),
                "encoded", department));
        User user = userRepository.saveAndFlush(User.create(
                "u" + unique + "@office.skhu.ac.kr", "u" + unique.substring(0, 12),
                "encoded", department));
        Petition petition = petitionRepository.saveAndFlush(Petition.create(
                writer, PetitionCategory.FACILITY, "concurrent bookmark", "content",
                10, LocalDateTime.now()));
        try {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<String>> futures = java.util.stream.IntStream.range(0, 2)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        try {
                            bookmarkService.create(user.getId(), petition.getId());
                            return "CREATED";
                        } catch (BookmarkException exception) {
                            return exception.getReason().name();
                        }
                    })).toList();
            ready.await();
            start.countDown();
            List<String> results = List.of(futures.get(0).get(), futures.get(1).get());
            executor.shutdown();

            assertThat(results).containsExactlyInAnyOrder(
                    "CREATED", BookmarkException.Reason.BOOKMARK_DUPLICATE.name());
            assertThat(bookmarkRepository.findByPetitionIdAndUserId(
                    petition.getId(), user.getId())).isPresent();
        } finally {
            bookmarkRepository.findByPetitionIdAndUserId(petition.getId(), user.getId())
                    .ifPresent(bookmarkRepository::delete);
            bookmarkRepository.flush();
            petitionRepository.deleteById(petition.getId());
            userRepository.deleteAllById(List.of(writer.getId(), user.getId()));
            departmentRepository.deleteById(department.getId());
        }
    }
}
