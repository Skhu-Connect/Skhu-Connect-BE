package org.skhuconnect.bookmark.entity;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.skhuconnect.global.entity.BaseEntity;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.user.entity.User;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BookmarkTest {

    @Test
    void hasDesignedJpaMapping() throws Exception {
        Table table = Bookmark.class.getAnnotation(Table.class);
        ManyToOne petition = Bookmark.class.getDeclaredField("petition")
                .getAnnotation(ManyToOne.class);
        ManyToOne user = Bookmark.class.getDeclaredField("user")
                .getAnnotation(ManyToOne.class);

        assertThat(Bookmark.class.getSuperclass()).isEqualTo(BaseEntity.class);
        assertThat(table.name()).isEqualTo("bookmarks");
        assertThat(Arrays.stream(table.uniqueConstraints())
                .map(constraint -> constraint.name()))
                .containsExactly("ux_bookmarks_petition_user");
        assertThat(Arrays.stream(table.indexes()).map(index -> index.name()))
                .containsExactly("ix_bookmarks_user_id_created_at");
        assertThat(petition.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(user.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(Modifier.isProtected(Bookmark.class
                .getDeclaredConstructor().getModifiers())).isTrue();
    }

    @Test
    void createsBookmarkWithPetitionAndUser() {
        Petition petition = mock(Petition.class);
        User user = mock(User.class);

        Bookmark bookmark = Bookmark.create(petition, user);

        assertThat(bookmark.getPetition()).isSameAs(petition);
        assertThat(bookmark.getUser()).isSameAs(user);
    }
}
