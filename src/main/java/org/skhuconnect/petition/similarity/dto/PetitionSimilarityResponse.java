package org.skhuconnect.petition.similarity.dto;

import java.util.List;

public record PetitionSimilarityResponse(
        double threshold,
        int totalElements,
        boolean cached,
        int remainingSearches,
        List<SimilarPetitionResponse> results
) {
    public static PetitionSimilarityResponse of(
            double threshold,
            boolean cached,
            int remainingSearches,
            List<SimilarPetitionResponse> results
    ) {
        return new PetitionSimilarityResponse(
                threshold,
                results.size(),
                cached,
                remainingSearches,
                results
        );
    }
}
