package org.skhuconnect.user.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record UserCommentPageResponse(
        List<UserCommentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static UserCommentPageResponse from(Page<UserCommentResponse> page) {
        return new UserCommentPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
