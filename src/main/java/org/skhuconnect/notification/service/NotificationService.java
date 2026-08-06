package org.skhuconnect.notification.service;

import org.skhuconnect.notification.dto.*;
import org.skhuconnect.notification.entity.Notification;
import org.skhuconnect.notification.exception.NotificationException;
import org.skhuconnect.notification.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class NotificationService {
    private static final int MAX_SIZE = 100;
    private final NotificationRepository repository;
    private final Clock clock;
    public NotificationService(NotificationRepository repository, Clock clock) {
        this.repository = repository; this.clock = clock;
    }
    @Transactional(readOnly = true)
    public NotificationPageResponse findAll(Long userId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_SIZE) throw new IllegalArgumentException("invalid page");
        return NotificationPageResponse.from(repository.findByReceiverId(userId,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))))
                .map(NotificationResponse::from));
    }
    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse unreadCount(Long userId) {
        return new UnreadNotificationCountResponse(repository.countByReceiverIdAndReadFalse(userId));
    }
    @Transactional
    public NotificationResponse markRead(Long userId, Long id) {
        Notification notification = repository.findByIdAndReceiverId(id, userId)
                .orElseThrow(NotificationException::new);
        notification.markRead(LocalDateTime.now(clock));
        return NotificationResponse.from(notification);
    }
    @Transactional
    public void markAllRead(Long userId) {
        repository.markAllRead(userId, LocalDateTime.now(clock));
    }
}
