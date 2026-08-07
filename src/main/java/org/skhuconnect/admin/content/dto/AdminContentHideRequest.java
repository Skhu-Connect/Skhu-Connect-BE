package org.skhuconnect.admin.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminContentHideRequest(
        @NotBlank @Size(max = 500) String hiddenReason
) {
}