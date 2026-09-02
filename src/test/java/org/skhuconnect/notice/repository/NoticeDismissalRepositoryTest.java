package org.skhuconnect.notice.repository;

import org.junit.jupiter.api.Test;
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.repository.AdminRepository;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.notice.entity.Notice;
import org.skhuconnect.notice.entity.NoticeDismissal;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notice-dismissal-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.mail.from=test@example.com",
        "app.mail.resend-api-key=re_test_key",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.cookie-secure=false"
})
@Transactional
class NoticeDismissalRepositoryTest {
    @Autowired private NoticeRepository noticeRepository;
    @Autowired private NoticeDismissalRepository dismissalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private DepartmentRepository departmentRepository;

    @Test
    void savesDismissalUniquely() {
        TestData data = saveData();
        dismissalRepository.saveAndFlush(NoticeDismissal.create(data.user(), data.oldNotice()));

        assertThat(dismissalRepository.existsByUserIdAndNoticeId(
                data.user().getId(), data.oldNotice().getId())).isTrue();
        assertThatThrownBy(() -> dismissalRepository.saveAndFlush(
                NoticeDismissal.create(data.user(), data.oldNotice())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void filtersBannerCandidates() {
        TestData data = saveData();
        dismissalRepository.saveAndFlush(NoticeDismissal.create(data.user(), data.oldNotice()));

        var page = noticeRepository.findUndismissedPublishedByUserId(
                data.user().getId(),
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("publishedAt"),
                        Sort.Order.desc("id"))));

        assertThat(page.getContent()).containsExactly(data.newNotice());
    }

    private TestData saveData() {
        String unique = UUID.randomUUID().toString().replace("-", "");
        Department department = departmentRepository.saveAndFlush(Department.create(
                "N" + unique.substring(0, 12), "notice-" + unique.substring(0, 12)));
        User user = userRepository.saveAndFlush(User.create(
                unique + "@office.skhu.ac.kr", "notice" + unique.substring(0, 12),
                "encoded", department));
        Admin admin = adminRepository.saveAndFlush(Admin.create(
                "admin" + unique.substring(0, 12), "encoded"));
        Notice oldNotice = Notice.create(admin, "이전 공지", "내용");
        oldNotice.publish();
        noticeRepository.saveAndFlush(oldNotice);
        Notice newNotice = Notice.create(admin, "새 공지", "내용");
        newNotice.publish();
        noticeRepository.saveAndFlush(newNotice);
        return new TestData(user, oldNotice, newNotice);
    }

    private record TestData(User user, Notice oldNotice, Notice newNotice) {
    }
}
