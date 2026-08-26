package org.skhuconnect.notification.repository;

import org.skhuconnect.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByEventKey(String eventKey);
    // Hiding or deleting a petition must also take its notifications out of the list: tapping one
    // would open a 404. Rows are kept - notifications are never deleted - and only filtered here.
    @Query(value = "select n from Notification n left join fetch n.petition p left join fetch n.comment"
            + " where n.receiver.id = :receiverId"
            + " and (p is null or (p.hidden = false and p.deleted = false))",
            countQuery = "select count(n) from Notification n left join n.petition p"
                    + " where n.receiver.id = :receiverId"
                    + " and (p is null or (p.hidden = false and p.deleted = false))")
    Page<Notification> findVisibleByReceiverId(@Param("receiverId") Long receiverId, Pageable pageable);
    Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId);
    @Query("select count(n) from Notification n left join n.petition p"
            + " where n.receiver.id = :receiverId and n.read = false"
            + " and (p is null or (p.hidden = false and p.deleted = false))")
    long countUnreadVisibleByReceiverId(@Param("receiverId") Long receiverId);
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.read = true, n.readAt = :now where n.receiver.id = :receiverId and n.read = false")
    int markAllRead(@Param("receiverId") Long receiverId, @Param("now") LocalDateTime now);
}
