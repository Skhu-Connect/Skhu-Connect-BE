package org.skhuconnect.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
import org.junit.jupiter.api.Test;
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.global.entity.BaseEntity;
import org.skhuconnect.notification.entity.NotificationPoint;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class UserTest {

    @Test
    void userHasDesignedJpaMapping() throws NoSuchFieldException {
        Table table = User.class.getAnnotation(Table.class);
        Field id = User.class.getDeclaredField("id");

        assertThat(User.class).hasAnnotation(Entity.class);
        assertThat(table).isNotNull();
        assertThat(table.name()).isEqualTo("users");
        assertThat(User.class.getSuperclass()).isEqualTo(BaseEntity.class);
        assertThat(id.getType()).isEqualTo(Long.class);
        assertThat(id.getAnnotation(Id.class)).isNotNull();
        assertThat(id.getAnnotation(GeneratedValue.class).strategy())
                .isEqualTo(GenerationType.IDENTITY);

        assertColumn("email", String.class, "email", 255, false);
        assertColumn("loginId", String.class, "login_id", 50, false);
        assertColumn("password", String.class, "password", 255, false);
        assertNotificationColumn("notificationEnabled", "notification_enabled");
        assertNotificationColumn("notifyAgreement", "notify_agreement");
        assertNotificationColumn("notifyAnswer", "notify_answer");
        assertNotificationColumn("notifyReply", "notify_reply");
        assertNotificationColumn("notifyLike", "notify_like");
        assertNotificationColumn("notifyNotice", "notify_notice");
        assertDepartmentMapping();
        assertIndexes(table.indexes());
    }

    @Test
    void userHasNoSetterAndProtectedNoArgsConstructor() throws NoSuchMethodException {
        assertThat(Arrays.stream(User.class.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("set")))
                .isEmpty();
        assertThat(Modifier.isProtected(
                User.class.getDeclaredConstructor().getModifiers()
        )).isTrue();
    }

    @Test
    void createStoresRequiredValuesAndEnablesNotifications() {
        Department department = Department.create("SOFTWARE", "소프트웨어융합학부");

        User user = User.create(
                "student@office.skhu.ac.kr",
                "student",
                "encoded-password",
                department
        );

        assertThat(user.getEmail()).isEqualTo("student@office.skhu.ac.kr");
        assertThat(user.getLoginId()).isEqualTo("student");
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        assertThat(user.getDepartment()).isSameAs(department);
        assertThat(user.isNotificationEnabled()).isTrue();
        assertThat(user.isNotifyAgreement()).isTrue();
        assertThat(user.isNotifyAnswer()).isTrue();
        assertThat(user.isNotifyReply()).isTrue();
        assertThat(user.isNotifyLike()).isTrue();
        assertThat(user.isNotifyNotice()).isTrue();
    }

    @Test
    void userChangesAllowedValues() {
        Department department = Department.create("SOFTWARE", "소프트웨어융합학부");
        Department changedDepartment = Department.create("IT", "IT융합자율학부");
        User user = User.create(
                "student@office.skhu.ac.kr",
                "student",
                "encoded-password",
                department
        );

        user.changePassword("changed-encoded-password");
        user.changeDepartment(changedDepartment);
        user.changeNotificationEnabled(false);
        user.changeNotificationSettings(null, null, null, false, null);

        assertThat(user.getPassword()).isEqualTo("changed-encoded-password");
        assertThat(user.getDepartment()).isSameAs(changedDepartment);
        assertThat(user.isNotificationEnabled()).isFalse();
        assertThat(user.allows(NotificationPoint.AGREEMENT)).isTrue();
        assertThat(user.allows(NotificationPoint.ANSWER)).isTrue();
        assertThat(user.allows(NotificationPoint.REPLY)).isTrue();
        assertThat(user.allows(NotificationPoint.LIKE)).isFalse();
        assertThat(user.allows(NotificationPoint.NOTICE)).isTrue();
    }

    @Test
    void createRejectsEmailOutsideSchoolDomain() {
        Department department = Department.create("SOFTWARE", "소프트웨어융합학부");

        assertThatIllegalArgumentException().isThrownBy(() -> User.create(
                "student@example.com",
                "student",
                "encoded-password",
                department
        ));
    }

    @Test
    void createRejectsNullRequiredValues() {
        Department department = Department.create("SOFTWARE", "소프트웨어융합학부");

        assertThatNullPointerException().isThrownBy(() -> User.create(
                null, "student", "encoded-password", department
        ));
        assertThatNullPointerException().isThrownBy(() -> User.create(
                "student@office.skhu.ac.kr", null, "encoded-password", department
        ));
        assertThatNullPointerException().isThrownBy(() -> User.create(
                "student@office.skhu.ac.kr", "student", null, department
        ));
        assertThatNullPointerException().isThrownBy(() -> User.create(
                "student@office.skhu.ac.kr", "student", "encoded-password", null
        ));
    }

    @Test
    void changeMethodsRejectNullRequiredValues() {
        Department department = Department.create("SOFTWARE", "소프트웨어융합학부");
        User user = User.create(
                "student@office.skhu.ac.kr",
                "student",
                "encoded-password",
                department
        );

        assertThatNullPointerException().isThrownBy(() -> user.changePassword(null));
        assertThatNullPointerException().isThrownBy(() -> user.changeDepartment(null));
    }

    private void assertColumn(
            String fieldName,
            Class<?> type,
            String columnName,
            int length,
            boolean unique
    ) throws NoSuchFieldException {
        Field field = User.class.getDeclaredField(fieldName);
        Column column = field.getAnnotation(Column.class);

        assertThat(field.getType()).isEqualTo(type);
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.length()).isEqualTo(length);
        assertThat(column.nullable()).isFalse();
        assertThat(column.unique()).isEqualTo(unique);
    }

    private void assertNotificationColumn(
            String fieldName,
            String columnName
    ) throws NoSuchFieldException {
        Field field = User.class.getDeclaredField(fieldName);
        Column column = field.getAnnotation(Column.class);

        assertThat(field.getType()).isEqualTo(boolean.class);
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isFalse();
        assertThat(field.getAnnotation(ColumnDefault.class).value()).isEqualTo("true");
    }

    private void assertDepartmentMapping() throws NoSuchFieldException {
        Field field = User.class.getDeclaredField("department");
        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);

        assertThat(field.getType()).isEqualTo(Department.class);
        assertThat(manyToOne).isNotNull();
        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isFalse();
        assertThat(joinColumn).isNotNull();
        assertThat(joinColumn.name()).isEqualTo("department_id");
        assertThat(joinColumn.nullable()).isFalse();
    }

    private void assertIndexes(Index[] indexes) {
        assertThat(indexes).extracting(Index::name, Index::columnList, Index::unique)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("ux_users_email", "email", true),
                        org.assertj.core.groups.Tuple.tuple(
                                "ux_users_login_id", "login_id", true
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                "ix_users_department_id", "department_id", false
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                "ix_users_deleted", "deleted", false
                        )
                );
    }
}
