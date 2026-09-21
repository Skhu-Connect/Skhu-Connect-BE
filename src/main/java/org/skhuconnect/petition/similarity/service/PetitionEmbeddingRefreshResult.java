package org.skhuconnect.petition.similarity.service;

public record PetitionEmbeddingRefreshResult(
        PetitionEmbeddingRefreshStatus status
) {
    public static PetitionEmbeddingRefreshResult success() {
        return new PetitionEmbeddingRefreshResult(PetitionEmbeddingRefreshStatus.SUCCESS);
    }

    public static PetitionEmbeddingRefreshResult failed() {
        return new PetitionEmbeddingRefreshResult(PetitionEmbeddingRefreshStatus.FAILED);
    }

    public static PetitionEmbeddingRefreshResult skipped() {
        return new PetitionEmbeddingRefreshResult(PetitionEmbeddingRefreshStatus.SKIPPED);
    }
}
