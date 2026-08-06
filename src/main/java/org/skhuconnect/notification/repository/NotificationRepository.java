package org.skhuconnect.notification.repository;

import org.skhuconnect.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByEventKey(String eventKey);
    @EntityGraph(attributePaths = {"petition", "comment"})
    Page<Notification> findByReceiverId(Long receiverId, Pageable pageable);
    Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId);
    long countByReceiverIdAndReadFalse(Long receiverId);
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.read = true, n.readAt = :now where n.receiver.id = :receiverId and n.read = false")
    int markAllRead(@Param("receiverId") Long receiverId, @Param("now") LocalDateTime now);
}
