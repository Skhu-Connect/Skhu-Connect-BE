package org.skhuconnect.notice.service;

import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.notificationlog.entity.NotificationLogTargetType;
import org.skhuconnect.admin.notificationlog.entity.NotificationLogType;
import org.skhuconnect.admin.notificationlog.service.AdminNotificationLogService;
import org.skhuconnect.admin.repository.AdminRepository;
import org.skhuconnect.notice.dto.NoticeCreateRequest;
import org.skhuconnect.notice.dto.NoticeResponse;
import org.skhuconnect.notice.entity.Notice;
import org.skhuconnect.notice.entity.NoticeDismissal;
import org.skhuconnect.notice.entity.NoticeStatus;
import org.skhuconnect.notice.exception.NoticeException;
import org.skhuconnect.notice.repository.NoticeDismissalRepository;
import org.skhuconnect.notice.repository.NoticeRepository;
import org.skhuconnect.notification.service.NotificationEventService;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class NoticeService {
    private static final int MAX_SIZE = 100;

    private final NoticeRepository notices;
    private final NoticeDismissalRepository dismissals;
    private final AdminRepository admins;
    private final UserRepository users;
    private final NotificationEventService events;
    private final AdminNotificationLogService logs;

    public NoticeService(
            NoticeRepository notices,
            NoticeDismissalRepository dismissals,
            AdminRepository admins,
            UserRepository users,
            NotificationEventService events,
            AdminNotificationLogService logs
    ) {
        this.notices = notices;
        this.dismissals = dismissals;
        this.admins = admins;
        this.users = users;
        this.events = events;
        this.logs = logs;
    }

    @Transactional
    public NoticeResponse create(Long adminId, NoticeCreateRequest request) {
        Admin admin = admins.findById(adminId).orElseThrow();
        return NoticeResponse.from(notices.save(Notice.create(
                admin,
                request.title(),
                request.content())));
    }

    @Transactional
    public NoticeResponse update(Long id, NoticeCreateRequest request) {
        Notice notice = notices.findById(id).orElseThrow();
        notice.update(request.title(), request.content());
        return NoticeResponse.from(notice);
    }

    @Transactional
    public NoticeResponse publish(Long adminId, Long id) {
        Notice notice = notices.findById(id).orElseThrow();
        if (notice.publish()) {
            events.onNoticePublished(notice.getId(), notice.getTitle(), users.findByDeletedFalse());
            Admin admin = admins.findById(adminId).orElseThrow();
            logs.record(NotificationLogType.NOTICE_PUBLISHED, admin,
                    NotificationLogTargetType.NOTICE, notice.getId(), "공지사항 최초 발행");
        }
        return NoticeResponse.from(notice);
    }

    @Transactional
    public NoticeResponse hide(Long id) {
        Notice notice = notices.findById(id).orElseThrow();
        notice.hide();
        return NoticeResponse.from(notice);
    }

    @Transactional
    public NoticeResponse republish(Long id) {
        Notice notice = notices.findById(id).orElseThrow();
        notice.publish();
        return NoticeResponse.from(notice);
    }

    @Transactional(readOnly = true)
    public Page<NoticeResponse> published(int page, int size) {
        validatePage(page, size);
        return notices.findByStatusOrderByCreatedAtDescIdDesc(
                NoticeStatus.PUBLISHED,
                PageRequest.of(page, size))
                .map(NoticeResponse::from);
    }

    @Transactional(readOnly = true)
    public Optional<NoticeResponse> banner(Long userId) {
        ensureUser(userId);
        return notices.findUndismissedPublishedByUserId(
                userId,
                PageRequest.of(0, 1, Sort.by(
                        Sort.Order.desc("publishedAt"),
                        Sort.Order.desc("id"))))
                .stream()
                .findFirst()
                .map(NoticeResponse::from);
    }

    public void dismiss(Long userId, Long noticeId) {
        User user = ensureUser(userId);
        Notice notice = notices.findByIdAndStatus(noticeId, NoticeStatus.PUBLISHED)
                .orElseThrow(() -> error(NoticeException.Reason.NOTICE_NOT_FOUND));
        if (dismissals.existsByUserIdAndNoticeId(userId, noticeId)) {
            return;
        }
        try {
            dismissals.saveAndFlush(NoticeDismissal.create(user, notice));
        } catch (DataIntegrityViolationException ignored) {
        }
    }

    private User ensureUser(Long userId) {
        return users.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> error(NoticeException.Reason.USER_NOT_FOUND));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw error(NoticeException.Reason.INVALID_PAGE);
        }
    }

    private NoticeException error(NoticeException.Reason reason) {
        return new NoticeException(reason);
    }
}
