package org.skhuconnect.notice.entity;

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
@Table(name = "notice_dismissals",
        uniqueConstraints = @UniqueConstraint(name = "ux_notice_dismissals_user_notice",
                columnNames = {"user_id", "notice_id"}),
        indexes = {
                @Index(name = "ix_notice_dismissals_user_created",
                        columnList = "user_id, created_at"),
                @Index(name = "ix_notice_dismissals_notice_id", columnList = "notice_id")
        })
public class NoticeDismissal extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    protected NoticeDismissal() {
    }

    private NoticeDismissal(User user, Notice notice) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.notice = Objects.requireNonNull(notice, "notice must not be null");
    }

    public static NoticeDismissal create(User user, Notice notice) {
        return new NoticeDismissal(user, notice);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Notice getNotice() {
        return notice;
    }
}
