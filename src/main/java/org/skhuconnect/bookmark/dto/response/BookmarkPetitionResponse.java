package org.skhuconnect.bookmark.dto.response;

import org.skhuconnect.bookmark.entity.Bookmark;
import org.skhuconnect.petition.dto.response.PetitionQueryResponse;

import java.time.LocalDateTime;

public record BookmarkPetitionResponse(
        Long bookmarkId,
        LocalDateTime bookmarkedAt,
        PetitionQueryResponse petition
) {
    public static BookmarkPetitionResponse from(
            Bookmark bookmark,
            LocalDateTime now
    ) {
        return new BookmarkPetitionResponse(
                bookmark.getId(),
                bookmark.getCreatedAt(),
                PetitionQueryResponse.from(bookmark.getPetition(), now)
        );
    }
}
