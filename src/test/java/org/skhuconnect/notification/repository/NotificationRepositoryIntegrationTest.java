package org.skhuconnect.notification.repository;

import org.junit.jupiter.api.Test;
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.repository.AdminRepository;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.department.repository.DepartmentRepository;
import org.skhuconnect.notification.entity.*;
import org.skhuconnect.petition.entity.*;
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
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "app.mail.from=test@example.com",
        "app.mail.resend-api-key=re_test_key",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.cookie-secure=false"
})
@Transactional
class NotificationRepositoryIntegrationTest {
    @Autowired NotificationRepository notifications;
    @Autowired DepartmentRepository departments;
    @Autowired UserRepository users;
    @Autowired PetitionRepository petitions;
    @Autowired AdminRepository admins;

    @Test void hiddenPetitionDropsItsNotificationsButKeepsNoticeOnes() {
        String u=UUID.randomUUID().toString().replace("-","");
        Department department=departments.saveAndFlush(Department.create("H"+u.substring(0,12),"hidden-"+u.substring(0,12)));
        User user=users.saveAndFlush(User.create(u+"@office.skhu.ac.kr","h"+u.substring(0,12),"encoded",department));
        Admin admin=admins.saveAndFlush(Admin.create("a"+u.substring(0,12),"encoded"));
        Petition petition=petitions.saveAndFlush(Petition.create(user, PetitionCategory.FACILITY,
                "hidden test","content",10, LocalDateTime.now()));
        notifications.saveAndFlush(Notification.create(user,NotificationType.PETITION_UNDER_REVIEW,
                petition,null,"hidden:"+u));
        notifications.saveAndFlush(Notification.createNotice(user,"공지","본문","notice:"+u));

        assertThat(notifications.findVisibleByReceiverId(user.getId(),PageRequest.of(0,20))).hasSize(2);
        assertThat(notifications.countUnreadVisibleByReceiverId(user.getId())).isEqualTo(2);

        petition.hide("운영 정책 위반",admin,LocalDateTime.now());
        petitions.saveAndFlush(petition);

        assertThat(notifications.findVisibleByReceiverId(user.getId(),PageRequest.of(0,20))).hasSize(1);
        assertThat(notifications.countUnreadVisibleByReceiverId(user.getId())).isEqualTo(1);
    }

    @Test void savesListsCountsAndRejectsDuplicateEventKey() {
        String u=UUID.randomUUID().toString().replace("-","");
        Department department=departments.saveAndFlush(Department.create("N"+u.substring(0,12),"notify-"+u.substring(0,12)));
        User user=users.saveAndFlush(User.create(u+"@office.skhu.ac.kr","n"+u.substring(0,12),"encoded",department));
        Petition petition=petitions.saveAndFlush(Petition.create(user, PetitionCategory.FACILITY,
                "notification test","content",10, LocalDateTime.now()));
        notifications.saveAndFlush(Notification.create(user,NotificationType.PETITION_UNDER_REVIEW,
                petition,null,"event:"+u));

        assertThat(notifications.countUnreadVisibleByReceiverId(user.getId())).isEqualTo(1);
        assertThat(notifications.findVisibleByReceiverId(user.getId(),PageRequest.of(0,20))).hasSize(1);
        assertThatThrownBy(()->notifications.saveAndFlush(Notification.create(user,
                NotificationType.PETITION_UNDER_REVIEW,petition,null,"event:"+u)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
