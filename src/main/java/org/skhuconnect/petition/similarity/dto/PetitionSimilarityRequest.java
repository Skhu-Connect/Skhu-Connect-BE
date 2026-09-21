package org.skhuconnect.petition.similarity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PetitionSimilarityRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank String content
) {
}
