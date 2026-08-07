package org.skhuconnect.admin.notificationlog.repository;
import org.skhuconnect.admin.notificationlog.entity.NotificationLog; import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificationLogRepository extends JpaRepository<NotificationLog,Long> {}