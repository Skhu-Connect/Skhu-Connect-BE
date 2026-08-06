package org.skhuconnect.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.skhuconnect.notification.dto.*;
import org.skhuconnect.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notification", description = "사용자 알림 API")
@RestController
@RequestMapping("/connect/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }

    @Operation(summary = "내 알림 목록 조회")
    @GetMapping
    public NotificationPageResponse findAll(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.findAll(userId, page, size);
    }
    @Operation(summary = "읽지 않은 알림 개수 조회")
    @GetMapping("/unread-count")
    public UnreadNotificationCountResponse unreadCount(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        return service.unreadCount(userId);
    }
    @Operation(summary = "알림 개별 읽음 처리")
    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markRead(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long notificationId) { return service.markRead(userId, notificationId); }
    @Operation(summary = "알림 전체 읽음 처리")
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        service.markAllRead(userId); return ResponseEntity.noContent().build();
    }
}
