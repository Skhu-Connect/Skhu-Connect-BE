package org.skhuconnect.notification.repository;

import org.junit.jupiter.api.Test;
import org.skhuconnect.notification.entity.*;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationRepositoryTest {
    @Test void entityDeclaresUniqueEventKeyAndReceiverReadIndex() {
        var table=Notification.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()).extracting(jakarta.persistence.UniqueConstraint::name)
                .contains("ux_notifications_event_key");
        assertThat(table.indexes()).extracting(jakarta.persistence.Index::name)
                .contains("ix_notifications_receiver_read_created");
    }
}
