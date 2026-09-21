package org.skhuconnect.petition.similarity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

public record PetitionSimilarityUsageResponse(
        int limit,
        int used,
        int remaining,
        long windowSeconds,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long retryAfterSeconds,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        LocalDateTime nextAvailableAt
) {
}
