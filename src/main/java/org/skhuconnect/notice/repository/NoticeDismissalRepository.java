package org.skhuconnect.notice.repository;

import org.skhuconnect.notice.entity.NoticeDismissal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeDismissalRepository extends JpaRepository<NoticeDismissal, Long> {
    boolean existsByUserIdAndNoticeId(Long userId, Long noticeId);
}
