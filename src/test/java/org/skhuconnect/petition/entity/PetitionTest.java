package org.skhuconnect.petition.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.skhuconnect.global.entity.BaseEntity;
import org.skhuconnect.user.entity.User;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PetitionTest {

    @Test
    void hasDesignedJpaMapping() throws Exception {
        Table table = Petition.class.getAnnotation(Table.class);
        ManyToOne writer = Petition.class.getDeclaredField("writer")
                .getAnnotation(ManyToOne.class);
        Column title = Petition.class.getDeclaredField("title")
                .getAnnotation(Column.class);

        assertThat(Petition.class.getSuperclass()).isEqualTo(BaseEntity.class);
        assertThat(table.name()).isEqualTo("petitions");
        assertThat(Arrays.stream(table.indexes()).map(Index::name)).containsExactly(
                "ix_petitions_status_created_at",
                "ix_petitions_category_created_at",
                "ix_petitions_hidden_deleted",
                "ix_petitions_writer_id_created_at",
                "ix_petitions_status_agreement_deadline"
        );
        assertThat(Petition.class.getDeclaredField("id")
                .getAnnotation(GeneratedValue.class).strategy())
                .isEqualTo(GenerationType.IDENTITY);
        assertThat(writer.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(writer.optional()).isFalse();
        assertThat(Petition.class.getDeclaredField("category")
                .getAnnotation(Enumerated.class).value()).isEqualTo(EnumType.STRING);
        assertThat(Petition.class.getDeclaredField("status")
                .getAnnotation(Enumerated.class).value()).isEqualTo(EnumType.STRING);
        assertThat(title.length()).isEqualTo(100);
        assertThat(title.nullable()).isFalse();
    }

    @Test
    void createsOpenPetitionWithThirtyDayDeadline() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 12, 0);

        Petition petition = createPetition(now);

        assertThat(petition.getWriter()).isNotNull();
        assertThat(petition.getCategory()).isEqualTo(PetitionCategory.FACILITY);
        assertThat(petition.getStatus()).isEqualTo(PetitionStatus.OPEN);
        assertThat(petition.getTitle()).isEqualTo("기존 제목");
        assertThat(petition.getContent()).isEqualTo("기존 내용");
        assertThat(petition.getAgreementCount()).isZero();
        assertThat(petition.getTargetAgreementCount()).isEqualTo(13);
        assertThat(petition.getAgreementDeadline()).isEqualTo(now.plusDays(30));
        assertThat(petition.isHidden()).isFalse();
        assertThat(petition.isDeleted()).isFalse();
    }

    @Test
    void updatesEditablePetition() {
        Petition petition = createPetition(LocalDateTime.now());

        petition.update("수정 제목", "수정 내용");

        assertThat(petition.getTitle()).isEqualTo("수정 제목");
        assertThat(petition.getContent()).isEqualTo("수정 내용");
    }

    @Test
    void softDeletesEditablePetition() {
        Petition petition = createPetition(LocalDateTime.now());
        LocalDateTime deletedAt = LocalDateTime.of(2026, 8, 5, 13, 0);

        petition.delete(deletedAt);

        assertThat(petition.isDeleted()).isTrue();
        assertThat(petition.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(petition.isEditable()).isFalse();
    }

    @Test
    void rejectsBlankContentAndNonPositiveTarget() {
        User writer = mock(User.class);
        LocalDateTime now = LocalDateTime.now();

        assertThatThrownBy(() -> Petition.create(
                writer, PetitionCategory.LIBRARY, " ", "내용", 5, now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Petition.create(
                writer, PetitionCategory.LIBRARY, "제목", "내용", 0, now))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void agreementAtTargetTransitionsOnceAndSetsReviewStartedAt() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 5, 12, 0);
        LocalDateTime agreedAt = createdAt.plusHours(1);
        Petition petition = Petition.create(
                mock(User.class), PetitionCategory.FACILITY,
                "title", "content", 1, createdAt);

        petition.addAgreement(agreedAt);

        assertThat(petition.getAgreementCount()).isEqualTo(1);
        assertThat(petition.getStatus()).isEqualTo(PetitionStatus.UNDER_REVIEW);
        assertThat(petition.getReviewStartedAt()).isEqualTo(agreedAt);
        assertThatThrownBy(() -> petition.addAgreement(agreedAt.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(petition.getAgreementCount()).isEqualTo(1);
        assertThat(petition.getReviewStartedAt()).isEqualTo(agreedAt);
    }

    @Test
    void underReviewCannotAddOrCancelAgreement() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 5, 12, 0);
        Petition petition = Petition.create(
                mock(User.class), PetitionCategory.FACILITY,
                "title", "content", 1, createdAt);
        petition.addAgreement(createdAt.plusHours(1));

        assertThatThrownBy(() ->
                petition.addAgreement(createdAt.plusHours(2)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() ->
                petition.removeAgreement(createdAt.plusHours(2)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void exactDeadlineIsNotAgreementOpen() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 5, 12, 0);
        Petition petition = createPetition(createdAt);

        assertThat(petition.isAgreementOpenAt(
                petition.getAgreementDeadline())).isFalse();
        assertThatThrownBy(() ->
                petition.addAgreement(petition.getAgreementDeadline()))
                .isInstanceOf(IllegalStateException.class);
    }
    @Test
    void exposesFieldsWithoutSetters() throws Exception {
        assertThat(Arrays.stream(Petition.class.getMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))).isEmpty();
        assertThat(Modifier.isProtected(Petition.class
                .getDeclaredConstructor().getModifiers())).isTrue();
    }

    private Petition createPetition(LocalDateTime now) {
        return Petition.create(
                mock(User.class),
                PetitionCategory.FACILITY,
                "기존 제목",
                "기존 내용",
                13,
                now
        );
    }
}
