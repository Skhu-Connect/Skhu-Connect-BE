package org.skhuconnect.notification.entity;

import jakarta.persistence.*;
import org.skhuconnect.global.entity.BaseEntity;
import org.skhuconnect.user.entity.User;

@Entity
@Table(name = "fcm_tokens", uniqueConstraints = @UniqueConstraint(name = "ux_fcm_tokens_token", columnNames = "token"), indexes = @Index(name = "ix_fcm_tokens_user_id", columnList = "user_id"))
public class FcmToken extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 512) private String token;
    protected FcmToken() {}
    private FcmToken(User user, String token) { this.user = user; this.token = token; }
    public static FcmToken create(User user, String token) { return new FcmToken(user, token); }
    public void changeUser(User user) { this.user = user; }
    public Long getId() { return id; } public User getUser() { return user; } public String getToken() { return token; }
}
