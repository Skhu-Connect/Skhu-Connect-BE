package org.skhuconnect.bookmark.repository;

import org.junit.jupiter.api.Test;
import org.skhuconnect.bookmark.entity.Bookmark;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.mail.host=localhost", "spring.mail.port=2525",
        "spring.mail.username=test", "spring.mail.password=test",
        "app.mail.from=test@example.com",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.cookie-secure=false"
})
@Transactional
class BookmarkRepositoryTest {

    @Autowired private BookmarkRepository bookmarkRepository;
    @Autowired private PetitionRepository petitionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DepartmentRepository departmentRepository;

    @Test
    void savesFindsAndEnforcesUniqueBookmark() {
        TestData data = saveData();
        bookmarkRepository.saveAndFlush(Bookmark.create(data.petition(), data.user()));

        assertThat(bookmarkRepository.existsByPetitionIdAndUserId(
                data.petition().getId(), data.user().getId())).isTrue();
        assertThat(bookmarkRepository.findByPetitionIdAndUserId(
                data.petition().getId(), data.user().getId())).isPresent();
        assertThatThrownBy(() -> bookmarkRepository.saveAndFlush(
                Bookmark.create(data.petition(), data.user())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void visibleListIsOwnedFilteredAndStableAcrossPages() {
        TestData data = saveData();
        User other = saveUser(data.department(), "other");
        LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < 5; index++) {
            Petition petition = petitionRepository.save(Petition.create(
                    data.writer(), PetitionCategory.FACILITY,
                    "bookmark-" + index, "content", 10, now));
            Bookmark bookmark = Bookmark.create(petition, data.user());
            bookmarkRepository.save(bookmark);
        }
        Petition hidden = Petition.create(data.writer(), PetitionCategory.FACILITY,
                "hidden", "content", 10, now);
        ReflectionTestUtils.setField(hidden, "hidden", true);
        petitionRepository.save(hidden);
        bookmarkRepository.save(Bookmark.create(hidden, data.user()));
        bookmarkRepository.save(Bookmark.create(data.petition(), other));
        bookmarkRepository.flush();

        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        var first = bookmarkRepository.findVisibleByUserId(
                data.user().getId(), PageRequest.of(0, 3, sort));
        var second = bookmarkRepository.findVisibleByUserId(
                data.user().getId(), PageRequest.of(1, 3, sort));
        var ids = new java.util.ArrayList<Long>();
        first.forEach(bookmark -> ids.add(bookmark.getId()));
        second.forEach(bookmark -> ids.add(bookmark.getId()));

        assertThat(ids).hasSize(5);
        assertThat(new HashSet<>(ids)).hasSize(5);
        assertThat(first.getTotalElements()).isEqualTo(5);
        assertThat(first.getContent()).extracting(Bookmark::getId).isSortedAccordingTo(
                java.util.Comparator.reverseOrder());
        assertThat(first.getContent()).allSatisfy(bookmark -> {
            assertThat(bookmark.getUser().getId()).isEqualTo(data.user().getId());
            assertThat(bookmark.getPetition().isHidden()).isFalse();
            assertThat(bookmark.getPetition().isDeleted()).isFalse();
        });
    }

    private TestData saveData() {
        String unique = UUID.randomUUID().toString().replace("-", "");
        Department department = departmentRepository.saveAndFlush(Department.create(
                "B" + unique.substring(0, 12), "bookmark-" + unique.substring(0, 12)));
        User writer = saveUser(department, "writer");
        User user = saveUser(department, "user");
        Petition petition = petitionRepository.saveAndFlush(Petition.create(
                writer, PetitionCategory.FACILITY, "bookmark test", "content", 10,
                LocalDateTime.now()));
        return new TestData(department, writer, petition, user);
    }

    private User saveUser(Department department, String prefix) {
        String unique = UUID.randomUUID().toString().replace("-", "");
        return userRepository.saveAndFlush(User.create(
                unique + "@office.skhu.ac.kr", prefix + unique.substring(0, 12),
                "encoded", department));
    }

    private record TestData(
            Department department, User writer, Petition petition, User user) {
    }
}
