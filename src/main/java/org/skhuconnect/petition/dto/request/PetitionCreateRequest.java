package org.skhuconnect.petition.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.skhuconnect.petition.entity.PetitionCategory;

public record PetitionCreateRequest(
        @NotNull PetitionCategory category,
        @NotBlank @Size(max = 100) String title,
        @NotBlank String content
) {
}
