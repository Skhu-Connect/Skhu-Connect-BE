package org.skhuconnect.user.dto;

import org.skhuconnect.comment.dto.response.CommentResponse;
import org.skhuconnect.comment.entity.Comment;

public record UserCommentResponse(
        Long petitionId,
        CommentResponse comment
) {
    public static UserCommentResponse from(
            Comment comment,
            long likeCount,
            Long userId,
            boolean liked
    ) {
        return new UserCommentResponse(
                comment.getPetition().getId(),
                CommentResponse.from(comment, likeCount, userId, liked)
        );
    }
}
