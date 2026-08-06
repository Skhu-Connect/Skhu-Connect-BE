package org.skhuconnect.agreement.entity;

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

class AgreementTest {

    @Test
    void hasDesignedJpaMapping() throws Exception {
        Table table = Agreement.class.getAnnotation(Table.class);
        ManyToOne petition = Agreement.class.getDeclaredField("petition")
                .getAnnotation(ManyToOne.class);
        ManyToOne user = Agreement.class.getDeclaredField("user")
                .getAnnotation(ManyToOne.class);

        assertThat(Agreement.class.getSuperclass()).isEqualTo(BaseEntity.class);
        assertThat(table.name()).isEqualTo("agreements");
        assertThat(Arrays.stream(table.uniqueConstraints())
                .map(constraint -> constraint.name()))
                .containsExactly("ux_agreements_petition_user");
        assertThat(Arrays.stream(table.indexes()).map(index -> index.name()))
                .containsExactly("ix_agreements_user_id_created_at");
        assertThat(petition.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(user.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(Modifier.isProtected(Agreement.class
                .getDeclaredConstructor().getModifiers())).isTrue();
    }

    @Test
    void createsAgreementWithPetitionAndUser() {
        Petition petition = mock(Petition.class);
        User user = mock(User.class);

        Agreement agreement = Agreement.create(petition, user);

        assertThat(agreement.getPetition()).isSameAs(petition);
        assertThat(agreement.getUser()).isSameAs(user);
    }
}