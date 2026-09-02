package org.skhuconnect.notice.entity;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.skhuconnect.global.entity.BaseEntity;
import org.skhuconnect.user.entity.User;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NoticeDismissalTest {
    @Test
    void hasDesignedJpaMapping() throws Exception {
        Table table = NoticeDismissal.class.getAnnotation(Table.class);
        ManyToOne user = NoticeDismissal.class.getDeclaredField("user")
                .getAnnotation(ManyToOne.class);
        ManyToOne notice = NoticeDismissal.class.getDeclaredField("notice")
                .getAnnotation(ManyToOne.class);

        assertThat(NoticeDismissal.class.getSuperclass()).isEqualTo(BaseEntity.class);
        assertThat(table.name()).isEqualTo("notice_dismissals");
        assertThat(Arrays.stream(table.uniqueConstraints())
                .map(constraint -> constraint.name()))
                .containsExactly("ux_notice_dismissals_user_notice");
        assertThat(Arrays.stream(table.indexes()).map(index -> index.name()))
                .containsExactly("ix_notice_dismissals_user_created",
                        "ix_notice_dismissals_notice_id");
        assertThat(user.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(notice.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(Modifier.isProtected(NoticeDismissal.class
                .getDeclaredConstructor().getModifiers())).isTrue();
    }

    @Test
    void createsDismissalWithUserAndNotice() {
        User user = mock(User.class);
        Notice notice = mock(Notice.class);

        NoticeDismissal dismissal = NoticeDismissal.create(user, notice);

        assertThat(dismissal.getUser()).isSameAs(user);
        assertThat(dismissal.getNotice()).isSameAs(notice);
    }
}
