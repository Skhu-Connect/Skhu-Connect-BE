package org.skhuconnect.petition.dto.request;

import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.entity.PetitionStatus;

public record PetitionQueryCondition(
        String keyword,
        PetitionCategory category,
        PetitionStatus status,
        int page,
        int size,
        String sort
) {
    public PetitionQueryCondition(
            String keyword,
            PetitionCategory category,
            PetitionStatus status
    ) {
        this(keyword, category, status, 0, 20, "createdAt,desc");
    }

    public String normalizedKeyword() {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }
}