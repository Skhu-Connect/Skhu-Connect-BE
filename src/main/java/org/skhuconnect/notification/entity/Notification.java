package org.skhuconnect.notification.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.global.entity.BaseEntity;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.user.entity.User;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "notifications",
        uniqueConstraints = @UniqueConstraint(name = "ux_notifications_event_key", columnNames = "event_key"),
        indexes = {
                @Index(name = "ix_notifications_receiver_read_created", columnList = "receiver_id, is_read, created_at"),
                @Index(name = "ix_notifications_petition_id", columnList = "petition_id"),
                @Index(name = "ix_notifications_comment_id", columnList = "comment_id")
        })
public class Notification extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "petition_id")
    private Petition petition;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;
    @Column(name = "event_key", nullable = false, length = 150)
    private String eventKey;
    @Column(name = "is_read", nullable = false) @ColumnDefault("false")
    private boolean read;
    @Column(name = "read_at")
    private LocalDateTime readAt;

    protected Notification() {}

    private Notification(User receiver, NotificationType type, Petition petition,
                         Comment comment, String eventKey) {
        this.receiver = Objects.requireNonNull(receiver);
        this.type = Objects.requireNonNull(type);
        this.petition = petition;
        this.comment = comment;
        this.eventKey = Objects.requireNonNull(eventKey);
        this.read = false;
    }

    public static Notification create(User receiver, NotificationType type, Petition petition,
                                      Comment comment, String eventKey) {
        return new Notification(receiver, type, petition, comment, eventKey);
    }

    public void markRead(LocalDateTime now) {
        if (!read) { read = true; readAt = Objects.requireNonNull(now); }
    }
    public Long getId() { return id; }
    public User getReceiver() { return receiver; }
    public NotificationType getType() { return type; }
    public Petition getPetition() { return petition; }
    public Comment getComment() { return comment; }
    public String getEventKey() { return eventKey; }
    public boolean isRead() { return read; }
    public LocalDateTime getReadAt() { return readAt; }
}
