package org.skhuconnect.admin.content.service;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.junit.jupiter.api.Test;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.petition.entity.Petition;

import static org.assertj.core.api.Assertions.assertThat;

class AdminContentJpaMappingTest {

    @Test
    void petitionAndCommentMapAdminHideProcessorAsLazyForeignKey() throws Exception {
        ManyToOne petitionAdmin = Petition.class.getDeclaredField("hiddenByAdmin")
                .getAnnotation(ManyToOne.class);
        JoinColumn petitionColumn = Petition.class.getDeclaredField("hiddenByAdmin")
                .getAnnotation(JoinColumn.class);
        ManyToOne commentAdmin = Comment.class.getDeclaredField("hiddenByAdmin")
                .getAnnotation(ManyToOne.class);
        JoinColumn commentColumn = Comment.class.getDeclaredField("hiddenByAdmin")
                .getAnnotation(JoinColumn.class);

        assertThat(petitionAdmin.fetch()).isEqualTo(jakarta.persistence.FetchType.LAZY);
        assertThat(petitionColumn.name()).isEqualTo("hidden_by_admin_id");
        assertThat(commentAdmin.fetch()).isEqualTo(jakarta.persistence.FetchType.LAZY);
        assertThat(commentColumn.name()).isEqualTo("hidden_by_admin_id");
    }
}