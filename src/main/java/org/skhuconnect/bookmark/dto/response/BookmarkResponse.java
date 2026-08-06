package org.skhuconnect.bookmark.dto.response;

public record BookmarkResponse(
        Long petitionId,
        boolean bookmarked
) {
    public static BookmarkResponse created(Long petitionId) {
        return new BookmarkResponse(petitionId, true);
    }
}
