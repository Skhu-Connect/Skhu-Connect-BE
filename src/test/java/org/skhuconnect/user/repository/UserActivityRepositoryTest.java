package org.skhuconnect.user.repository;

import org.junit.jupiter.api.Test;
import org.skhuconnect.agreement.repository.AgreementRepository;
import org.skhuconnect.comment.repository.CommentRepository;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.springframework.data.jpa.repository.Query;

import static org.assertj.core.api.Assertions.assertThat;

class UserActivityRepositoryTest {

    @Test
    void petitionAndAgreementQueriesRestrictOwnerAndVisiblePetitions() throws Exception {
        Query petitions = PetitionRepository.class
                .getMethod("findVisibleByWriterId", Long.class,
                        org.springframework.data.domain.Pageable.class)
                .getAnnotation(Query.class);
        Query agreements = AgreementRepository.class
                .getMethod("findVisibleByUserId", Long.class,
                        org.springframework.data.domain.Pageable.class)
                .getAnnotation(Query.class);

        assertThat(petitions.value())
                .contains("petition.writer.id = :userId")
                .contains("petition.deleted = false")
                .contains("petition.hidden = false");
        assertThat(agreements.value())
                .contains("agreement.user.id = :userId")
                .contains("agreement.petition.deleted = false")
                .contains("agreement.petition.hidden = false");
    }

    @Test
    void commentQueryExcludesDeletedCommentsAndInvisiblePetitions()
            throws Exception {
        Query comments = CommentRepository.class
                .getMethod("findVisibleActivityByWriterId", Long.class,
                        org.springframework.data.domain.Pageable.class)
                .getAnnotation(Query.class);

        assertThat(comments.value())
                .contains("comment.writer.id = :userId")
                .contains("comment.deleted = false")
                .doesNotContain("comment.hidden = false")
                .contains("comment.petition.deleted = false")
                .contains("comment.petition.hidden = false");
    }
}
