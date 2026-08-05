package org.skhuconnect.petition.dto.response;

import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.entity.PetitionStatus;

import java.time.LocalDateTime;

public record PetitionResponse(
        Long id,
        PetitionCategory category,
        PetitionStatus status,
        String title,
        String content,
        int agreementCount,
        int targetAgreementCount,
        LocalDateTime agreementDeadline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PetitionResponse from(Petition petition) {
        return new PetitionResponse(
                petition.getId(),
                petition.getCategory(),
                petition.getStatus(),
                petition.getTitle(),
                petition.getContent(),
                petition.getAgreementCount(),
                petition.getTargetAgreementCount(),
                petition.getAgreementDeadline(),
                petition.getCreatedAt(),
                petition.getUpdatedAt()
        );
    }
}
