package org.skhuconnect.notification.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skhuconnect.notification.entity.NotificationPoint.*;
import static org.skhuconnect.notification.entity.NotificationType.*;

class NotificationTypeTest {

    @Test
    void everyTypeMapsToItsNotificationPoint() {
        assertThat(PETITION_AGREEMENT_60_PERCENT.point()).isEqualTo(AGREEMENT);
        assertThat(PETITION_AGREEMENT_100_PERCENT.point()).isEqualTo(AGREEMENT);
        assertThat(PETITION_UNDER_REVIEW.point()).isEqualTo(AGREEMENT);
        assertThat(PETITION_ANSWERED.point()).isEqualTo(ANSWER);
        assertThat(COMMENT_REPLY.point()).isEqualTo(REPLY);
        assertThat(COMMENT_LIKE.point()).isEqualTo(LIKE);
        assertThat(REPLY_LIKE.point()).isEqualTo(LIKE);
        assertThat(NotificationType.NOTICE.point()).isEqualTo(NotificationPoint.NOTICE);
    }
}
