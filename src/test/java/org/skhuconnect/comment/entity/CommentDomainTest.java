package org.skhuconnect.comment.entity;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class CommentDomainTest {

    @Test
    void entitiesHaveDesignedMappings() throws Exception {
        Table mappingTable = PetitionAnonymousNumber.class.getAnnotation(Table.class);
        Table likeTable = CommentLike.class.getAnnotation(Table.class);
        ManyToOne commentMapping = Comment.class.getDeclaredField("anonymousNumber")
                .getAnnotation(ManyToOne.class);

        assertThat(Comment.class.getSuperclass()).isEqualTo(BaseEntity.class);
        assertThat(PetitionAnonymousNumber.class.getSuperclass()).isEqualTo(BaseEntity.class);
        assertThat(CommentLike.class.getSuperclass()).isEqualTo(BaseEntity.class);
        assertThat(Arrays.stream(mappingTable.uniqueConstraints())
                .map(constraint -> constraint.name()))
                .containsExactlyInAnyOrder(
                        "ux_petition_anonymous_numbers_petition_user",
                        "ux_petition_anonymous_numbers_petition_number");
        assertThat(Arrays.stream(likeTable.uniqueConstraints())
                .map(constraint -> constraint.name()))
                .containsExactly("ux_comment_likes_comment_user");
        assertThat(commentMapping.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(Modifier.isProtected(Comment.class
                .getDeclaredConstructor().getModifiers())).isTrue();
    }

    @Test
    void commentRequiresMappingForSamePetitionAndWriter() {
        Petition petition = mock(Petition.class);
        Petition otherPetition = mock(Petition.class);
        User writer = mock(User.class);
        User otherUser = mock(User.class);
        org.mockito.Mockito.when(petition.getId()).thenReturn(1L);
        org.mockito.Mockito.when(otherPetition.getId()).thenReturn(2L);
        org.mockito.Mockito.when(writer.getId()).thenReturn(1L);
        org.mockito.Mockito.when(otherUser.getId()).thenReturn(2L);
        PetitionAnonymousNumber mapping = PetitionAnonymousNumber.create(
                petition, writer, 1);

        Comment comment = Comment.create(petition, writer, mapping, "content");

        assertThat(comment.getAnonymousNumber()).isSameAs(mapping);
        assertThat(comment.getAnonymousNumber().getAnonymousNumber()).isEqualTo(1);
        assertThatThrownBy(() -> Comment.create(
                otherPetition, writer, mapping, "content"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Comment.create(
                petition, otherUser, mapping, "content"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateAndLogicalDeleteValidateState() {
        Petition petition = mock(Petition.class);
        User writer = mock(User.class);
        Comment comment = Comment.create(petition, writer,
                PetitionAnonymousNumber.create(petition, writer, 1), "before");

        comment.update("after");
        comment.delete(java.time.LocalDateTime.of(2026, 8, 6, 12, 0));

        assertThat(comment.getContent()).isEqualTo("after");
        assertThat(comment.isDeleted()).isTrue();
        assertThatThrownBy(() -> comment.update("again"))
                .isInstanceOf(IllegalStateException.class);
    }
}
