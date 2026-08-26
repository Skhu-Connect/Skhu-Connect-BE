package org.skhuconnect.notification.entity;

public enum NotificationType {
    PETITION_AGREEMENT_60_PERCENT(NotificationPoint.AGREEMENT),
    PETITION_AGREEMENT_100_PERCENT(NotificationPoint.AGREEMENT),
    PETITION_UNDER_REVIEW(NotificationPoint.AGREEMENT),
    PETITION_ANSWERED(NotificationPoint.ANSWER),
    COMMENT_REPLY(NotificationPoint.REPLY),
    COMMENT_LIKE(NotificationPoint.LIKE),
    REPLY_LIKE(NotificationPoint.LIKE),
    NOTICE(NotificationPoint.NOTICE);

    private final NotificationPoint point;

    NotificationType(NotificationPoint point) {
        this.point = point;
    }

    public NotificationPoint point() {
        return point;
    }
}
