package org.skhuconnect.admin.content.dto;

import org.skhuconnect.comment.entity.Comment;

import java.time.LocalDateTime;

public record AdminCommentResponse(
        Long id,
        Long parentCommentId,
        Long writerId,
        boolean writerDeleted,
        String content,
        int anonymousNumber,
        boolean hidden,
        String hiddenReason,
        Long hiddenByAdminId,
        LocalDateTime hiddenAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminCommentResponse from(Comment comment) {
        return new AdminCommentResponse(comment.getId(),
                comment.getParentComment() == null ? null : comment.getParentComment().getId(),
                comment.getWriter().getId(), comment.getWriter().isDeleted(),
                comment.getContent(), comment.getAnonymousNumber().getAnonymousNumber(),
                comment.isHidden(), comment.getHiddenReason(),
                comment.getHiddenByAdmin() == null ? null : comment.getHiddenByAdmin().getId(),
                comment.getHiddenAt(), comment.getCreatedAt(), comment.getUpdatedAt());
    }
}