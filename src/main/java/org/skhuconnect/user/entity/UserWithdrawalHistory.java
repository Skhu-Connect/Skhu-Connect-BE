package org.skhuconnect.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.skhuconnect.global.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_withdrawal_histories", indexes = {
        @Index(name = "ux_user_withdrawal_histories_user_id",
                columnList = "user_id", unique = true),
        @Index(name = "ix_user_withdrawal_histories_email_hash_withdrawn_at",
                columnList = "email_hash, withdrawn_at")
})
public class UserWithdrawalHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "email_hash", nullable = false, length = 64)
    private String emailHash;

    @Column(name = "withdrawn_at", nullable = false)
    private LocalDateTime withdrawnAt;

    protected UserWithdrawalHistory() {
    }

    private UserWithdrawalHistory(User user, String emailHash, LocalDateTime withdrawnAt) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.emailHash = Objects.requireNonNull(emailHash, "emailHash must not be null");
        this.withdrawnAt = Objects.requireNonNull(withdrawnAt, "withdrawnAt must not be null");
    }

    public static UserWithdrawalHistory create(
            User user, String emailHash, LocalDateTime withdrawnAt) {
        return new UserWithdrawalHistory(user, emailHash, withdrawnAt);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getEmailHash() {
        return emailHash;
    }

    public LocalDateTime getWithdrawnAt() {
        return withdrawnAt;
    }
}
