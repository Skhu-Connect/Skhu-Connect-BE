package org.skhuconnect.notification.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.global.entity.BaseEntity;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.user.entity.User;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(name = "ux_notifications_event_key", columnNames = "event_key"), indexes = {@Index(name = "ix_notifications_receiver_read_created", columnList = "receiver_id, is_read, created_at"), @Index(name = "ix_notifications_petition_id", columnList = "petition_id"), @Index(name = "ix_notifications_comment_id", columnList = "comment_id")})
public class Notification extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "receiver_id", nullable = false) private User receiver;
    // String 로 매핑한다(NotificationType 필드 대신) - Hibernate 6+ 는 프로퍼티의 자바 타입이
    // enum 이면(@Enumerated·컨버터 여부와 무관하게) 그 시점 enum 값 목록으로 DB CHECK 제약을
    // 자동 생성하는데, ddl-auto=update 는 기존 CHECK 제약을 절대 갱신하지 않는다. 나중에
    // NotificationType 에 값을 추가하면 그 CHECK 가 새 값을 계속 막아 매 요청이 500 으로
    // 끊긴다(2026-08-30 실제 장애, notifications_chk_1 - ARCHITECTURE.md 참고). Hibernate 에게
    // 이 컬럼을 순수 String 으로 보여줘야 그 자동 생성 로직을 아예 안 탄다.
    @Column(name = "type", nullable = false, length = 50) private String typeName;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "petition_id") private Petition petition;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "comment_id") private Comment comment;
    @Column(name = "event_key", nullable = false, length = 150) private String eventKey;
    @Column(name = "title", length = 200) private String title;
    @Column(name = "body", length = 5000, columnDefinition = "TEXT") private String body;
    @Column(name = "is_read", nullable = false) @ColumnDefault("false") private boolean read;
    @Column(name = "read_at") private LocalDateTime readAt;
    protected Notification() {}
    private Notification(User receiver, NotificationType type, Petition petition, Comment comment, String eventKey, String title, String body) {
        this.receiver = Objects.requireNonNull(receiver); this.typeName = Objects.requireNonNull(type).name(); this.petition = petition; this.comment = comment; this.eventKey = Objects.requireNonNull(eventKey); this.title = title; this.body = body; this.read = false;
    }
    public static Notification create(User receiver, NotificationType type, Petition petition, Comment comment, String eventKey) { return new Notification(receiver, type, petition, comment, eventKey, null, null); }
    public static Notification createNotice(User receiver, String title, String body, String eventKey) { return new Notification(receiver, NotificationType.NOTICE, null, null, eventKey, title, body); }
    public void markRead(LocalDateTime now) { if (!read) { read = true; readAt = Objects.requireNonNull(now); } }
    public Long getId() { return id; } public User getReceiver() { return receiver; } public NotificationType getType() { return NotificationType.valueOf(typeName); } public Petition getPetition() { return petition; } public Comment getComment() { return comment; } public String getEventKey() { return eventKey; } public String getTitle() { return title; } public String getBody() { return body; } public boolean isRead() { return read; } public LocalDateTime getReadAt() { return readAt; }
}
