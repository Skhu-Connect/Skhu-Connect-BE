package org.skhuconnect.admin.token.entity;

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
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.global.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "admin_refresh_tokens", indexes = {
        @Index(name = "ux_admin_refresh_tokens_admin_id", columnList = "admin_id", unique = true),
        @Index(name = "ux_admin_refresh_tokens_token_hash", columnList = "token_hash", unique = true)
})
public class AdminRefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false, unique = true)
    private Admin admin;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    protected AdminRefreshToken() {
    }

    private AdminRefreshToken(Admin admin, String tokenHash, LocalDateTime expiresAt) {
        this.admin = Objects.requireNonNull(admin);
        rotate(tokenHash, expiresAt);
    }

    public static AdminRefreshToken create(Admin admin, String tokenHash, LocalDateTime expiresAt) {
        return new AdminRefreshToken(admin, tokenHash, expiresAt);
    }

    public void rotate(String tokenHash, LocalDateTime expiresAt) {
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt);
    }

    public boolean isExpiredAt(LocalDateTime now) { return !now.isBefore(expiresAt); }
    public Admin getAdmin() { return admin; }
    public String getTokenHash() { return tokenHash; }
}