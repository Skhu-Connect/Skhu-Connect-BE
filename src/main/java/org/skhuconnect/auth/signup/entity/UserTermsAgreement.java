package org.skhuconnect.auth.signup.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.skhuconnect.user.entity.User;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "user_terms_agreements",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_user_terms_agreements_user_version",
                columnNames = {"user_id", "terms_version"}
        )
)
public class UserTermsAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "terms_version", nullable = false, length = 20)
    private String termsVersion;

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    protected UserTermsAgreement() {
    }

    private UserTermsAgreement(User user, String termsVersion, LocalDateTime agreedAt) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.termsVersion = Objects.requireNonNull(
                termsVersion, "termsVersion must not be null");
        this.agreedAt = Objects.requireNonNull(agreedAt, "agreedAt must not be null");
    }

    public static UserTermsAgreement create(
            User user, String termsVersion, LocalDateTime agreedAt
    ) {
        return new UserTermsAgreement(user, termsVersion, agreedAt);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTermsVersion() {
        return termsVersion;
    }

    public LocalDateTime getAgreedAt() {
        return agreedAt;
    }
}
