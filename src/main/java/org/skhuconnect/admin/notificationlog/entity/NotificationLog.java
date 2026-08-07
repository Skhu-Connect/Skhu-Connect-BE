package org.skhuconnect.admin.notificationlog.entity;
import jakarta.persistence.*; import org.skhuconnect.admin.entity.Admin; import org.skhuconnect.global.entity.BaseEntity; import java.util.Objects;
@Entity @Table(name="notification_logs", indexes={@Index(name="ix_notification_logs_type_created_at",columnList="type, created_at"),@Index(name="ix_notification_logs_created_at",columnList="created_at")})
public class NotificationLog extends BaseEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=50) private NotificationLogType type;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="admin_id") private Admin admin;
 @Enumerated(EnumType.STRING) @Column(name="target_type",nullable=false,length=30) private NotificationLogTargetType targetType;
 @Column(name="target_id",nullable=false) private Long targetId;
 @Column(nullable=false,length=1000) private String description;
 protected NotificationLog(){}
 private NotificationLog(NotificationLogType type,Admin admin,NotificationLogTargetType targetType,Long targetId,String description){this.type=Objects.requireNonNull(type);this.admin=admin;this.targetType=Objects.requireNonNull(targetType);this.targetId=Objects.requireNonNull(targetId);this.description=requireDescription(description);}
 public static NotificationLog create(NotificationLogType type,Admin admin,NotificationLogTargetType targetType,Long targetId,String description){return new NotificationLog(type,admin,targetType,targetId,description);}
 private static String requireDescription(String value){Objects.requireNonNull(value);if(value.isBlank()||value.length()>1000)throw new IllegalArgumentException("invalid description");return value;}
 public Long getId(){return id;} public NotificationLogType getType(){return type;} public Admin getAdmin(){return admin;} public NotificationLogTargetType getTargetType(){return targetType;} public Long getTargetId(){return targetId;} public String getDescription(){return description;}
}