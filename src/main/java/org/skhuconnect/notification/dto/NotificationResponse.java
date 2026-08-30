package org.skhuconnect.notification.dto;

import org.skhuconnect.notification.entity.Notification;
import org.skhuconnect.notification.entity.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(Long id, NotificationType type, String message,
        Long petitionId, Long commentId, boolean read, LocalDateTime createdAt) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getBody() != null ? n.getBody() : message(n),
                n.getPetition() == null ? null : n.getPetition().getId(),
                n.getComment() == null ? null : n.getComment().getId(),
                n.isRead(), n.getCreatedAt());
    }
    public static String message(Notification n) {
        return switch (n.getType()) {
            case PETITION_AGREEMENT_60_PERCENT -> "청원이 목표 동의 수의 60%에 도달했습니다.";
            case PETITION_AGREEMENT_100_PERCENT -> "청원이 목표 동의 수를 달성했습니다.";
            case PETITION_UNDER_REVIEW -> "청원 검토가 시작되었습니다.";
            case PETITION_ANSWERED -> "청원에 공식 답변이 등록되었습니다.";
            case COMMENT_REPLY -> "내 댓글에 대댓글이 작성되었습니다.";
            case COMMENT_LIKE -> "내 댓글에 공감이 등록되었습니다.";
            case REPLY_LIKE -> "내 대댓글에 공감이 등록되었습니다.";
            case NOTICE -> "새 공지사항이 등록되었습니다";
            case REPORT_DISMISSED -> "신고하신 내용이 검토 후 기각되었습니다.";
            case REPORT_ACTION_TAKEN -> "신고하신 내용에 대해 조치가 완료되었습니다.";
            case CONTENT_HIDDEN -> n.getComment() != null ? "작성하신 댓글이 신고 처리로 숨김 처리되었습니다." : "작성하신 글이 신고 처리로 숨김 처리되었습니다.";
            case ACCOUNT_LOGIN_BANNED -> "이용 정책 위반으로 로그인이 제한되었습니다.";
        };
    }
}
