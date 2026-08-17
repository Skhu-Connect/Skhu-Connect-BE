package org.skhuconnect.user.block.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.skhuconnect.global.entity.BaseEntity;
import org.skhuconnect.user.entity.User;

import java.util.Objects;

@Entity
@Table(name = "user_blocks",
        uniqueConstraints = @UniqueConstraint(name = "ux_user_blocks_blocker_blocked",
                columnNames = {"blocker_id", "blocked_user_id"}),
        indexes = @Index(name = "ix_user_blocks_blocked_user_id", columnList = "blocked_user_id"))
public class UserBlock extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_user_id", nullable = false)
    private User blockedUser;

    protected UserBlock() { }

    private UserBlock(User blocker, User blockedUser) {
        this.blocker = Objects.requireNonNull(blocker, "blocker must not be null");
        this.blockedUser = Objects.requireNonNull(blockedUser, "blockedUser must not be null");
    }

    public static UserBlock create(User blocker, User blockedUser) {
        return new UserBlock(blocker, blockedUser);
    }

    public Long getId() { return id; }
    public User getBlocker() { return blocker; }
    public User getBlockedUser() { return blockedUser; }
}
