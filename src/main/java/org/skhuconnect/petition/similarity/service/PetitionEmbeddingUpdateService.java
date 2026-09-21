package org.skhuconnect.petition.similarity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.petition.similarity.client.EmbeddingClient;
import org.skhuconnect.petition.similarity.client.EmbeddingClientException;
import org.skhuconnect.petition.similarity.client.EmbeddingResult;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PetitionEmbeddingUpdateService {

    private static final Logger log = LoggerFactory.getLogger(PetitionEmbeddingUpdateService.class);

    private final PetitionRepository petitions;
    private final EmbeddingClient embeddingClient;
    private final PetitionEmbeddingPersistenceService persistenceService;

    public PetitionEmbeddingUpdateService(
            PetitionRepository petitions,
            EmbeddingClient embeddingClient,
            PetitionEmbeddingPersistenceService persistenceService
    ) {
        this.petitions = petitions;
        this.embeddingClient = embeddingClient;
        this.persistenceService = persistenceService;
    }

    public PetitionEmbeddingRefreshResult refresh(Long petitionId) {
        Optional<Petition> petition = petitions.findByIdAndDeletedFalseAndHiddenFalse(petitionId);
        if (petition.isEmpty()) {
            return PetitionEmbeddingRefreshResult.skipped();
        }
        String text = PetitionEmbeddingText.from(petition.get());
        String contentHash = PetitionContentHasher.sha256(text);
        try {
            EmbeddingResult result = embeddingClient.embed(text);
            persistenceService.saveReady(petition.get(), contentHash, result);
            return PetitionEmbeddingRefreshResult.success();
        } catch (EmbeddingClientException exception) {
            persistenceService.saveFailed(petition.get(), contentHash, exception.getMessage());
            log.warn("petition embedding refresh failed: petitionId={}, reason={}",
                    petitionId, exception.getMessage());
            return PetitionEmbeddingRefreshResult.failed();
        }
    }
}
