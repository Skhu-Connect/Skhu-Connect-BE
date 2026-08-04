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
import org.skhuconnect.department.entity.Department;
import org.skhuconnect.global.entity.BaseEntity;

import java.util.Objects;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "ux_users_email", columnList = "email", unique = true),
                @Index(name = "ux_users_login_id", columnList = "login_id", unique = true),
                @Index(name = "ix_users_department_id", columnList = "department_id")
        }
)
public class User extends BaseEntity {

    private static final String SCHOOL_EMAIL_SUFFIX = "@office.skhu.ac.kr";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "notification_enabled", nullable = false)
    @ColumnDefault("true")
    private boolean notificationEnabled;

    protected User() {
    }

    private User(
            String email,
            String loginId,
            String encodedPassword,
            Department department
    ) {
        validateSchoolEmail(email);
        this.email = email;
        this.loginId = Objects.requireNonNull(loginId, "loginId must not be null");
        this.password = Objects.requireNonNull(
                encodedPassword,
                "encodedPassword must not be null"
        );
        this.department = Objects.requireNonNull(department, "department must not be null");
        this.notificationEnabled = true;
    }

    public static User create(
            String email,
            String loginId,
            String encodedPassword,
            Department department
    ) {
        return new User(email, loginId, encodedPassword, department);
    }

    public void changePassword(String encodedPassword) {
        this.password = Objects.requireNonNull(
                encodedPassword,
                "encodedPassword must not be null"
        );
    }

    public void changeDepartment(Department department) {
        this.department = Objects.requireNonNull(department, "department must not be null");
    }

    public void changeNotificationEnabled(boolean enabled) {
        this.notificationEnabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPassword() {
        return password;
    }

    public Department getDepartment() {
        return department;
    }

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    private static void validateSchoolEmail(String email) {
        Objects.requireNonNull(email, "email must not be null");
        if (!email.endsWith(SCHOOL_EMAIL_SUFFIX)) {
            throw new IllegalArgumentException("email must end with " + SCHOOL_EMAIL_SUFFIX);
        }
    }
}
