package org.skhuconnect.notice.repository;

import org.skhuconnect.notice.entity.Notice;
import org.skhuconnect.notice.entity.NoticeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    Page<Notice> findByStatusOrderByCreatedAtDescIdDesc(NoticeStatus status, Pageable pageable);

    Optional<Notice> findByIdAndStatus(Long id, NoticeStatus status);

    @Query("select n from Notice n"
            + " where n.status = org.skhuconnect.notice.entity.NoticeStatus.PUBLISHED"
            + " and not exists (select d.id from NoticeDismissal d"
            + " where d.notice = n and d.user.id = :userId)")
    Page<Notice> findUndismissedPublishedByUserId(
            @Param("userId") Long userId,
            Pageable pageable);
}
