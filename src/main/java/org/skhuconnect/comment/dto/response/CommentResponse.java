package org.skhuconnect.comment.dto.response;

import org.skhuconnect.comment.entity.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        String content,
        int anonymousNumber,
        long likeCount,
        boolean myComment,
        boolean liked,
        boolean hidden,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    private static final String HIDDEN_CONTENT = "관리자에 의해 숨김 처리된 댓글입니다.";

    public static CommentResponse from(
            Comment comment,
            long likeCount,
            Long userId,
            boolean liked
    ) {
        return new CommentResponse(
                comment.getId(),
                comment.isHidden() ? HIDDEN_CONTENT : comment.getContent(),
                comment.getAnonymousNumber().getAnonymousNumber(),
                likeCount,
                userId != null && comment.isWrittenBy(userId),
                userId != null && liked,
                comment.isHidden(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
