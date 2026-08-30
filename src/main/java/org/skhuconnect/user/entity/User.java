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
import org.skhuconnect.notification.entity.NotificationPoint;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "ux_users_email", columnList = "email", unique = true),
                @Index(name = "ux_users_login_id", columnList = "login_id", unique = true),
                @Index(name = "ix_users_department_id", columnList = "department_id"),
                @Index(name = "ix_users_deleted", columnList = "deleted")
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

    @Column(name = "notify_agreement", nullable = false)
    @ColumnDefault("true")
    private boolean notifyAgreement = true;

    @Column(name = "notify_answer", nullable = false)
    @ColumnDefault("true")
    private boolean notifyAnswer = true;

    @Column(name = "notify_reply", nullable = false)
    @ColumnDefault("true")
    private boolean notifyReply = true;

    @Column(name = "notify_like", nullable = false)
    @ColumnDefault("true")
    private boolean notifyLike = true;

    @Column(name = "notify_notice", nullable = false)
    @ColumnDefault("true")
    private boolean notifyNotice = true;

    @Column(name = "deleted", nullable = false)
    @ColumnDefault("false")
    private boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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
        this.notifyAgreement = true;
        this.notifyAnswer = true;
        this.notifyReply = true;
        this.notifyLike = true;
        this.notifyNotice = true;
        this.deleted = false;
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

    public void changeLoginId(String loginId) {
        this.loginId = Objects.requireNonNull(loginId, "loginId must not be null");
    }

    public void changeDepartment(Department department) {
        this.department = Objects.requireNonNull(department, "department must not be null");
    }

    public void changeNotificationEnabled(boolean enabled) {
        this.notificationEnabled = enabled;
    }

    public void changeNotificationSettings(
            Boolean agreement,
            Boolean answer,
            Boolean reply,
            Boolean like,
            Boolean notice
    ) {
        if (agreement != null) notifyAgreement = agreement;
        if (answer != null) notifyAnswer = answer;
        if (reply != null) notifyReply = reply;
        if (like != null) notifyLike = like;
        if (notice != null) notifyNotice = notice;
    }

    public boolean allows(NotificationPoint point) {
        return switch (point) {
            case AGREEMENT -> notifyAgreement;
            case ANSWER -> notifyAnswer;
            case REPLY -> notifyReply;
            case LIKE -> notifyLike;
            case NOTICE -> notifyNotice;
        };
    }

    public void withdraw(LocalDateTime withdrawnAt) {
        if (deleted) {
            throw new IllegalStateException("user is already withdrawn");
        }
        if (id == null) {
            throw new IllegalStateException("persisted user is required");
        }
        this.email = "withdrawn-" + id + "@deleted.invalid";
        this.loginId = "withdrawn-" + id;
        this.password = "WITHDRAWN:" + id;
        this.notificationEnabled = false;
        this.notifyAgreement = false;
        this.notifyAnswer = false;
        this.notifyReply = false;
        this.notifyLike = false;
        this.notifyNotice = false;
        this.deleted = true;
        this.deletedAt = Objects.requireNonNull(withdrawnAt, "withdrawnAt must not be null");
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

    public boolean isNotifyAgreement() {
        return notifyAgreement;
    }

    public boolean isNotifyAnswer() {
        return notifyAnswer;
    }

    public boolean isNotifyReply() {
        return notifyReply;
    }

    public boolean isNotifyLike() {
        return notifyLike;
    }

    public boolean isNotifyNotice() {
        return notifyNotice;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    private static void validateSchoolEmail(String email) {
        Objects.requireNonNull(email, "email must not be null");
        if (!email.endsWith(SCHOOL_EMAIL_SUFFIX)) {
            throw new IllegalArgumentException("email must end with " + SCHOOL_EMAIL_SUFFIX);
        }
    }
}
