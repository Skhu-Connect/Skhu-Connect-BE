package org.skhuconnect.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record CommentCreateRequest(
        @NotBlank @Size(max = 1000) String content,
        @Schema(description = "Parent root comment ID; null creates a root comment", nullable = true)
        Long parentCommentId
) {
    public CommentCreateRequest(String content) { this(content, null); }
}