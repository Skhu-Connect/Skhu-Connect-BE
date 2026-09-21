package org.skhuconnect.petition.similarity.service;

public record PetitionEmbeddingBackfillResult(
        int totalCandidates,
        int successCount,
        int failureCount,
        int skippedCount
) {
}
