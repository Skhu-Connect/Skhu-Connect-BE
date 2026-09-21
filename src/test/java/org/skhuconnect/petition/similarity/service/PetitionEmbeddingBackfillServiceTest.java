package org.skhuconnect.petition.similarity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.petition.similarity.config.OpenAiProperties;
import org.skhuconnect.petition.similarity.entity.PetitionEmbedding;
import org.skhuconnect.petition.similarity.repository.PetitionEmbeddingRepository;
import org.skhuconnect.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PetitionEmbeddingBackfillServiceTest {

    private PetitionRepository petitions;
    private PetitionEmbeddingRepository embeddings;
    private PetitionEmbeddingUpdateService updateService;
    private PetitionEmbeddingBackfillService service;

    @BeforeEach
    void setUp() {
        petitions = mock(PetitionRepository.class);
        embeddings = mock(PetitionEmbeddingRepository.class);
        updateService = mock(PetitionEmbeddingUpdateService.class);
        service = new PetitionEmbeddingBackfillService(
                petitions,
                embeddings,
                updateService,
                new OpenAiProperties()
        );
    }

    @Test
    void backfillsOnlyMissingOrOutdatedPublicPetitionsAndCountsResults() {
        Petition current = petition(10L, "현재 제목", "현재 본문");
        Petition missing = petition(11L, "누락 제목", "누락 본문");
        Petition failed = petition(12L, "실패 제목", "실패 본문");
        when(petitions.findPublicPetitionsForEmbedding(any(Pageable.class)))
                .thenReturn(List.of(current, missing, failed));
        when(embeddings.findByPetitionId(10L))
                .thenReturn(Optional.of(currentEmbedding(current)));
        when(embeddings.findByPetitionId(11L)).thenReturn(Optional.empty());
        when(embeddings.findByPetitionId(12L)).thenReturn(Optional.empty());
        when(updateService.refresh(11L)).thenReturn(PetitionEmbeddingRefreshResult.success());
        when(updateService.refresh(12L)).thenReturn(PetitionEmbeddingRefreshResult.failed());

        PetitionEmbeddingBackfillResult result = service.backfillPublicPetitions(100);

        assertThat(result.totalCandidates()).isEqualTo(3);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
        verify(updateService, never()).refresh(10L);
        verify(updateService).refresh(11L);
        verify(updateService).refresh(12L);
    }

    @Test
    void rejectsInvalidLimit() {
        assertThatThrownBy(() -> service.backfillPublicPetitions(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be between 1 and 1000");
        assertThatThrownBy(() -> service.backfillPublicPetitions(1001))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be between 1 and 1000");
    }

    private PetitionEmbedding currentEmbedding(Petition petition) {
        String hash = PetitionContentHasher.sha256(PetitionEmbeddingText.from(petition));
        return PetitionEmbedding.ready(
                petition,
                "text-embedding-3-small",
                256,
                hash,
                EmbeddingVectorCodec.encode(new float[256]),
                LocalDateTime.of(2026, 8, 5, 12, 0)
        );
    }

    private Petition petition(Long id, String title, String content) {
        Petition petition = Petition.create(
                mock(User.class),
                PetitionCategory.FACILITY,
                title,
                content,
                10,
                LocalDateTime.of(2026, 8, 5, 12, 0)
        );
        ReflectionTestUtils.setField(petition, "id", id);
        return petition;
    }
}
