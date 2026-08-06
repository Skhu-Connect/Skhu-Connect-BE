package org.skhuconnect.comment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.skhuconnect.global.entity.BaseEntity;
import org.skhuconnect.user.entity.User;

import java.util.Objects;

@Entity
@Table(
        name = "comment_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_comment_likes_comment_user",
                columnNames = {"comment_id", "user_id"}
        )
)
public class CommentLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected CommentLike() {
    }

    private CommentLike(Comment comment, User user) {
        this.comment = Objects.requireNonNull(comment, "comment must not be null");
        this.user = Objects.requireNonNull(user, "user must not be null");
    }

    public static CommentLike create(Comment comment, User user) {
        return new CommentLike(comment, user);
    }

    public Long getId() { return id; }
    public Comment getComment() { return comment; }
    public User getUser() { return user; }
}
