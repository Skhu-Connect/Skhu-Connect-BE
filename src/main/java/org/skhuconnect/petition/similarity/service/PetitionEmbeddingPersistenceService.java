package org.skhuconnect.petition.similarity.service;

import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.similarity.client.EmbeddingResult;
import org.skhuconnect.petition.similarity.config.OpenAiProperties;
import org.skhuconnect.petition.similarity.entity.PetitionEmbedding;
import org.skhuconnect.petition.similarity.repository.PetitionEmbeddingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class PetitionEmbeddingPersistenceService {

    private final PetitionEmbeddingRepository embeddings;
    private final OpenAiProperties openAiProperties;
    private final Clock clock;

    public PetitionEmbeddingPersistenceService(
            PetitionEmbeddingRepository embeddings,
            OpenAiProperties openAiProperties,
            Clock clock
    ) {
        this.embeddings = embeddings;
        this.openAiProperties = openAiProperties;
        this.clock = clock;
    }

    @Transactional
    public void saveReady(Petition petition, String contentHash, EmbeddingResult result) {
        PetitionEmbedding embedding = embeddings.findByPetitionId(petition.getId())
                .orElse(null);
        byte[] encoded = EmbeddingVectorCodec.encode(result.vector());
        LocalDateTime now = LocalDateTime.now(clock);
        if (embedding == null) {
            embeddings.save(PetitionEmbedding.ready(
                    petition, result.modelName(), result.dimensions(), contentHash, encoded, now));
            return;
        }
        embedding.updateReady(
                result.modelName(), result.dimensions(), contentHash, encoded, now);
    }

    @Transactional
    public void saveFailed(Petition petition, String contentHash, String message) {
        PetitionEmbedding embedding = embeddings.findByPetitionId(petition.getId())
                .orElse(null);
        LocalDateTime now = LocalDateTime.now(clock);
        if (embedding == null) {
            embeddings.save(PetitionEmbedding.failed(
                    petition,
                    openAiProperties.getEmbeddingModel(),
                    openAiProperties.getEmbeddingDimensions(),
                    contentHash,
                    message,
                    now));
            return;
        }
        embedding.updateFailed(
                openAiProperties.getEmbeddingModel(),
                openAiProperties.getEmbeddingDimensions(),
                contentHash,
                message,
                now);
    }
}
