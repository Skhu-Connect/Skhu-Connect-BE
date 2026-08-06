package org.skhuconnect.comment.repository;

import org.junit.jupiter.api.Test;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.comment.entity.CommentLike;
import org.skhuconnect.comment.entity.PetitionAnonymousNumber;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
class CommentRepositoryTest {

    @Autowired private CommentRepository commentRepository;
    @Autowired private PetitionAnonymousNumberRepository mappingRepository;
    @Autowired private CommentLikeRepository likeRepository;
    @Autowired private PetitionRepository petitionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DepartmentRepository departmentRepository;

    @Test
    void mappingUserUniqueConstraintRejectsDuplicate() {
        TestData data = saveData();
        mappingRepository.saveAndFlush(PetitionAnonymousNumber.create(
                data.petition(), data.user(), 1));

        assertThatThrownBy(() -> mappingRepository.saveAndFlush(
                PetitionAnonymousNumber.create(data.petition(), data.user(), 2)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void commentLikeUniqueConstraintRejectsDuplicate() {
        TestData data = saveData();
        PetitionAnonymousNumber mapping = mappingRepository.saveAndFlush(
                PetitionAnonymousNumber.create(data.petition(), data.user(), 1));
        Comment comment = commentRepository.saveAndFlush(Comment.create(
                data.petition(), data.user(), mapping, "content"));
        likeRepository.saveAndFlush(CommentLike.create(comment, data.user()));

        assertThatThrownBy(() -> likeRepository.saveAndFlush(
                CommentLike.create(comment, data.user())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameAnonymousNumberCannotBeAssignedTwiceInPetition() {
        TestData data = saveData();
        User other = saveUser(data.department(), "other");
        mappingRepository.saveAndFlush(PetitionAnonymousNumber.create(
                data.petition(), data.user(), 1));

        assertThatThrownBy(() -> mappingRepository.saveAndFlush(
                PetitionAnonymousNumber.create(data.petition(), other, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletedCommentsAreExcludedAndLikeAggregatesAreReturned() {
        TestData data = saveData();
        PetitionAnonymousNumber mapping = mappingRepository.saveAndFlush(
                PetitionAnonymousNumber.create(data.petition(), data.user(), 1));
        Comment visible = commentRepository.save(Comment.create(
                data.petition(), data.user(), mapping, "visible"));
        Comment deleted = Comment.create(data.petition(), data.user(), mapping, "deleted");
        deleted.delete(LocalDateTime.now());
        commentRepository.saveAndFlush(deleted);
        likeRepository.saveAndFlush(CommentLike.create(visible, data.user()));

        var page = commentRepository.findByPetitionIdAndDeletedFalse(
                data.petition().getId(), PageRequest.of(0, 20));
        var counts = likeRepository.countByCommentIds(
                java.util.List.of(visible.getId(), deleted.getId()));

        assertThat(page.getContent()).extracting(Comment::getId)
                .containsExactly(visible.getId());
        assertThat(counts).singleElement().satisfies(count -> {
            assertThat(count.getCommentId()).isEqualTo(visible.getId());
            assertThat(count.getLikeCount()).isEqualTo(1);
        });
    }

    private TestData saveData() {
        String unique = UUID.randomUUID().toString().replace("-", "");
        Department department = departmentRepository.saveAndFlush(Department.create(
                "M" + unique.substring(0, 12), "comment-" + unique.substring(0, 12)));
        User writer = saveUser(department, "writer");
        User user = saveUser(department, "user");
        Petition petition = petitionRepository.saveAndFlush(Petition.create(
                writer, PetitionCategory.FACILITY, "comment test", "content", 10,
                LocalDateTime.now()));
        return new TestData(department, petition, user);
    }

    private User saveUser(Department department, String prefix) {
        String unique = UUID.randomUUID().toString().replace("-", "");
        return userRepository.saveAndFlush(User.create(
                unique + "@office.skhu.ac.kr", prefix + unique.substring(0, 12),
                "encoded", department));
    }

    private record TestData(Department department, Petition petition, User user) {
    }
}
