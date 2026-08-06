package org.skhuconnect.comment.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.skhuconnect.global.entity.BaseEntity;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.user.entity.User;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "comments", indexes = {
        @Index(name = "ix_comments_petition_id_created_at", columnList = "petition_id, created_at"),
        @Index(name = "ix_comments_writer_id_created_at", columnList = "writer_id, created_at"),
        @Index(name = "ix_comments_anonymous_number_id", columnList = "anonymous_number_id"),
        @Index(name = "ix_comments_parent_comment_id_created_at", columnList = "parent_comment_id, created_at"),
        @Index(name = "ix_comments_hidden_deleted", columnList = "hidden, deleted")
})
public class Comment extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "petition_id", nullable = false)
    private Petition petition;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "writer_id", nullable = false)
    private User writer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "anonymous_number_id", nullable = false)
    private PetitionAnonymousNumber anonymousNumber;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;
    @Column(name = "content", nullable = false, length = 1000)
    private String content;
    @Column(name = "hidden", nullable = false) @ColumnDefault("false")
    private boolean hidden;
    @Column(name = "hidden_reason", length = 500)
    private String hiddenReason;
    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;
    @Column(name = "hidden_by_admin_id")
    private Long hiddenByAdminId;
    @Column(name = "deleted", nullable = false) @ColumnDefault("false")
    private boolean deleted;
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Comment() {}

    private Comment(Petition petition, User writer, PetitionAnonymousNumber anonymousNumber,
                    Comment parentComment, String content) {
        this.petition = Objects.requireNonNull(petition, "petition must not be null");
        this.writer = Objects.requireNonNull(writer, "writer must not be null");
        this.anonymousNumber = Objects.requireNonNull(anonymousNumber, "anonymousNumber must not be null");
        if (!anonymousNumber.matches(petition, writer)) throw new IllegalArgumentException("anonymousNumber mapping does not match comment");
        if (parentComment != null) {
            if (!parentComment.getPetition().getId().equals(petition.getId())) throw new IllegalArgumentException("parent comment belongs to another petition");
            if (parentComment.isReply()) throw new IllegalArgumentException("reply depth cannot exceed one");
        }
        this.parentComment = parentComment;
        this.content = requireContent(content);
        this.hidden = false;
        this.deleted = false;
    }

    public static Comment create(Petition petition, User writer, PetitionAnonymousNumber anonymousNumber, String content) {
        return new Comment(petition, writer, anonymousNumber, null, content);
    }
    public static Comment create(Petition petition, User writer, PetitionAnonymousNumber anonymousNumber,
                                 Comment parentComment, String content) {
        return new Comment(petition, writer, anonymousNumber, parentComment, content);
    }
    public void update(String content) {
        if (deleted) throw new IllegalStateException("deleted comment cannot be updated");
        this.content = requireContent(content);
    }
    public void delete(LocalDateTime deletedAt) {
        if (deleted) throw new IllegalStateException("comment is already deleted");
        this.deleted = true;
        this.deletedAt = Objects.requireNonNull(deletedAt, "deletedAt must not be null");
    }
    public boolean isWrittenBy(Long userId) { return writer.getId().equals(userId); }
    public boolean isReply() { return parentComment != null; }
    private static String requireContent(String content) {
        Objects.requireNonNull(content, "content must not be null");
        if (content.isBlank()) throw new IllegalArgumentException("content must not be blank");
        if (content.length() > 1000) throw new IllegalArgumentException("content must not exceed 1000 characters");
        return content;
    }
    public Long getId() { return id; }
    public Petition getPetition() { return petition; }
    public User getWriter() { return writer; }
    public PetitionAnonymousNumber getAnonymousNumber() { return anonymousNumber; }
    public Comment getParentComment() { return parentComment; }
    public String getContent() { return content; }
    public boolean isHidden() { return hidden; }
    public String getHiddenReason() { return hiddenReason; }
    public LocalDateTime getHiddenAt() { return hiddenAt; }
    public Long getHiddenByAdminId() { return hiddenByAdminId; }
    public boolean isDeleted() { return deleted; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}