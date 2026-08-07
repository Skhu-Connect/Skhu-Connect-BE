package org.skhuconnect.admin.answer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.skhuconnect.admin.answer.entity.AnswerSource;

public record OfficialAnswerRequest(
        @NotBlank @Size(max = 1000) String content,
        @NotNull AnswerSource answerSource
) {
}