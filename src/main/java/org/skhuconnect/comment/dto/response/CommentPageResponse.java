package org.skhuconnect.comment.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record CommentPageResponse(
        List<CommentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static CommentPageResponse from(Page<CommentResponse> page) {
        return new CommentPageResponse(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(),
                page.isFirst(), page.isLast()
        );
    }
}
