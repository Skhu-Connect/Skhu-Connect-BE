package org.skhuconnect.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UserWithdrawalRequest(
        @Schema(description = "?? ????", example = "current-password")
        @NotBlank String password
) {
}
