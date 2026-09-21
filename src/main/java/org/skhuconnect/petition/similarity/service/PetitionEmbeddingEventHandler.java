package org.skhuconnect.petition.similarity.service;

import org.skhuconnect.petition.similarity.event.PetitionEmbeddingRefreshRequestedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PetitionEmbeddingEventHandler {

    private final PetitionEmbeddingUpdateService updateService;

    public PetitionEmbeddingEventHandler(PetitionEmbeddingUpdateService updateService) {
        this.updateService = updateService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PetitionEmbeddingRefreshRequestedEvent event) {
        updateService.refresh(event.petitionId());
    }
}
