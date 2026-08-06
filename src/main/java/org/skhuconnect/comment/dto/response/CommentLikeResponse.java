package org.skhuconnect.comment.dto.response;

public record CommentLikeResponse(
        Long commentId,
        long likeCount,
        boolean liked
) {
}
