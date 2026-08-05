package org.skhuconnect.petition.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PetitionUpdateRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank String content
) {
}
