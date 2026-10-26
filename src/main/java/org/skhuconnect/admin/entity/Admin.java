package org.skhuconnect.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.skhuconnect.global.entity.BaseEntity;

import java.util.Objects;

@Entity
@Table(name = "admins", indexes = {
        @Index(name = "ux_admins_login_id", columnList = "login_id", unique = true)
})
public class Admin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, length = 50, unique = true)
    private String loginId;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "role", length = 30)
    private String role;

    protected Admin() {
    }

    private Admin(String loginId, String encodedPassword) {
        this.loginId = Objects.requireNonNull(loginId);
        this.password = Objects.requireNonNull(encodedPassword);
        this.role = "STUDENT_COUNCIL";
    }

    public static Admin create(String loginId, String encodedPassword) {
        return new Admin(loginId, encodedPassword);
    }

    public static Admin createEmergencyCommittee(String loginId, String encodedPassword) {
        Admin admin = new Admin(loginId, encodedPassword);
        admin.role = "EMERGENCY_COMMITTEE";
        return admin;
    }

    public String getRole() {
        return role == null ? "STUDENT_COUNCIL" : role;
    }

    public Long getId() { return id; }
    public String getLoginId() { return loginId; }
    public String getPassword() { return password; }
}
