package org.skhuconnect.petition.similarity.dto;

import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.entity.PetitionStatus;

import java.time.LocalDateTime;

public record SimilarPetitionResponse(
        Long id,
        PetitionCategory category,
        PetitionStatus status,
        String title,
        String content,
        int agreementCount,
        int targetAgreementCount,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        double similarity
) {
    public static SimilarPetitionResponse from(
            Petition petition,
            PetitionStatus effectiveStatus,
            double similarity
    ) {
        return new SimilarPetitionResponse(
                petition.getId(),
                petition.getCategory(),
                effectiveStatus,
                petition.getTitle(),
                petition.getContent(),
                petition.getAgreementCount(),
                petition.getTargetAgreementCount(),
                petition.getAgreementDeadline(),
                petition.getCreatedAt(),
                petition.getUpdatedAt(),
                similarity
        );
    }
}
