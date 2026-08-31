package org.skhuconnect.notification.entity;

public enum NotificationType {
    PETITION_AGREEMENT_60_PERCENT(NotificationPoint.AGREEMENT),
    PETITION_AGREEMENT_100_PERCENT(NotificationPoint.AGREEMENT),
    PETITION_UNDER_REVIEW(NotificationPoint.AGREEMENT),
    PETITION_ANSWERED(NotificationPoint.ANSWER),
    PETITION_COMMENT_CREATED(NotificationPoint.REPLY),
    COMMENT_REPLY(NotificationPoint.REPLY),
    COMMENT_LIKE(NotificationPoint.LIKE),
    REPLY_LIKE(NotificationPoint.LIKE),
    NOTICE(NotificationPoint.NOTICE),
    REPORT_DISMISSED(NotificationPoint.REPORT),
    REPORT_ACTION_TAKEN(NotificationPoint.REPORT),
    CONTENT_HIDDEN(NotificationPoint.REPORT),
    ACCOUNT_LOGIN_BANNED(NotificationPoint.REPORT);

    private final NotificationPoint point;

    NotificationType(NotificationPoint point) {
        this.point = point;
    }

    public NotificationPoint point() {
        return point;
    }
}
