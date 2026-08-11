package org.skhuconnect.comment.dto.response;

import org.skhuconnect.comment.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        @Schema(description = "Parent root comment ID; null for root comments", nullable = true)
        Long parentCommentId, String content, int anonymousNumber,
        long likeCount, boolean myComment, boolean liked, boolean hidden,
        LocalDateTime createdAt, LocalDateTime updatedAt,
        @Schema(description = "Replies of a root comment; omitted for replies")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<CommentResponse> replies
) {
    public CommentResponse(Long id, String content, int anonymousNumber, long likeCount,
            boolean myComment, boolean liked, boolean hidden,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, null, content, anonymousNumber, likeCount, myComment, liked, hidden,
                createdAt, updatedAt, List.of());
    }

    private static final String HIDDEN_CONTENT = "관리자에 의해 숨김 처리된 댓글입니다.";
    private static final String DELETED_CONTENT = "삭제된 댓글입니다.";

    public static CommentResponse from(Comment comment, long likeCount, Long userId, boolean liked) {
        return from(comment, likeCount, userId, liked, List.of());
    }
    public static CommentResponse from(Comment comment, long likeCount, Long userId,
                                       boolean liked, List<CommentResponse> replies) {
        return new CommentResponse(comment.getId(),
                comment.getParentComment() == null ? null : comment.getParentComment().getId(),
                contentOf(comment), comment.getAnonymousNumber().getAnonymousNumber(), likeCount,
                userId != null && comment.isWrittenBy(userId), userId != null && liked,
                comment.isHidden(), comment.getCreatedAt(), comment.getUpdatedAt(),
                comment.isReply() ? null : List.copyOf(replies));
    }
    private static String contentOf(Comment comment) {
        if (comment.isDeleted()) return DELETED_CONTENT;
        return comment.isHidden() ? HIDDEN_CONTENT : comment.getContent();
    }
}