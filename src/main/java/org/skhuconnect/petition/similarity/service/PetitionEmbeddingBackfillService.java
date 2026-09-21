package org.skhuconnect.petition.similarity.service;

import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.petition.similarity.config.OpenAiProperties;
import org.skhuconnect.petition.similarity.repository.PetitionEmbeddingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PetitionEmbeddingBackfillService {

    private final PetitionRepository petitions;
    private final PetitionEmbeddingRepository embeddings;
    private final PetitionEmbeddingUpdateService updateService;
    private final OpenAiProperties openAiProperties;

    public PetitionEmbeddingBackfillService(
            PetitionRepository petitions,
            PetitionEmbeddingRepository embeddings,
            PetitionEmbeddingUpdateService updateService,
            OpenAiProperties openAiProperties
    ) {
        this.petitions = petitions;
        this.embeddings = embeddings;
        this.updateService = updateService;
        this.openAiProperties = openAiProperties;
    }

    public PetitionEmbeddingBackfillResult backfillPublicPetitions(int limit) {
        if (limit <= 0 || limit > 1000) {
            throw new IllegalArgumentException("limit must be between 1 and 1000");
        }
        List<Petition> candidates = petitions.findPublicPetitionsForEmbedding(
                PageRequest.of(0, limit));
        int success = 0;
        int failure = 0;
        int skipped = 0;
        for (Petition petition : candidates) {
            String hash = PetitionContentHasher.sha256(PetitionEmbeddingText.from(petition));
            boolean current = embeddings.findByPetitionId(petition.getId())
                    .filter(embedding -> embedding.isReadyFor(
                            openAiProperties.getEmbeddingModel(),
                            openAiProperties.getEmbeddingDimensions(),
                            hash))
                    .isPresent();
            if (!current) {
                PetitionEmbeddingRefreshResult result = updateService.refresh(petition.getId());
                if (result.status() == PetitionEmbeddingRefreshStatus.SUCCESS) {
                    success++;
                } else if (result.status() == PetitionEmbeddingRefreshStatus.FAILED) {
                    failure++;
                } else {
                    skipped++;
                }
            } else {
                skipped++;
            }
        }
        return new PetitionEmbeddingBackfillResult(
                candidates.size(), success, failure, skipped);
    }
}
