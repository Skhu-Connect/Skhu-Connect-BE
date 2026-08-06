package org.skhuconnect.bookmark.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record BookmarkPageResponse(
        List<BookmarkPetitionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static BookmarkPageResponse from(Page<BookmarkPetitionResponse> page) {
        return new BookmarkPageResponse(
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
