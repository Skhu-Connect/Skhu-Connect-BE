package org.skhuconnect.admin.answer.entity;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.petition.entity.Petition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class OfficialAnswerTest {

    @Test
    void mapsOneAnswerPerPetitionAndLastProcessingAdmin() throws Exception {
        Table table = OfficialAnswer.class.getAnnotation(Table.class);
        OneToOne petition = OfficialAnswer.class.getDeclaredField("petition").getAnnotation(OneToOne.class);
        JoinColumn petitionColumn = OfficialAnswer.class.getDeclaredField("petition").getAnnotation(JoinColumn.class);
        ManyToOne admin = OfficialAnswer.class.getDeclaredField("admin").getAnnotation(ManyToOne.class);

        assertThat(table.name()).isEqualTo("official_answers");
        assertThat(table.indexes()[0].name()).isEqualTo("ux_official_answers_petition_id");
        assertThat(table.indexes()[0].unique()).isTrue();
        assertThat(petition.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(petitionColumn.name()).isEqualTo("petition_id");
        assertThat(petitionColumn.unique()).isTrue();
        assertThat(admin.fetch()).isEqualTo(FetchType.LAZY);
    }

    @Test
    void updatesContentSourceAndLastProcessingAdmin() {
        Petition petition = mock(Petition.class);
        Admin firstAdmin = mock(Admin.class);
        Admin updatedAdmin = mock(Admin.class);
        OfficialAnswer answer = OfficialAnswer.create(petition, firstAdmin, "first answer", AnswerSource.OPERATION_TEAM);

        answer.update(updatedAdmin, "updated answer", AnswerSource.SCHOOL_OFFICIAL);

        assertThat(answer.getAdmin()).isSameAs(updatedAdmin);
        assertThat(answer.getContent()).isEqualTo("updated answer");
        assertThat(answer.getAnswerSource()).isEqualTo(AnswerSource.SCHOOL_OFFICIAL);
    }

    @Test
    void rejectsBlankOrOverlongContent() {
        assertThatThrownBy(() -> OfficialAnswer.create(mock(Petition.class), mock(Admin.class), " ", AnswerSource.OPERATION_TEAM))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OfficialAnswer.create(mock(Petition.class), mock(Admin.class), "a".repeat(1001), AnswerSource.OPERATION_TEAM))
                .isInstanceOf(IllegalArgumentException.class);
    }
}