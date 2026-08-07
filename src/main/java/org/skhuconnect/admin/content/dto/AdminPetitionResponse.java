package org.skhuconnect.admin.content.dto;

import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.entity.PetitionStatus;

import java.time.LocalDateTime;

public record AdminPetitionResponse(
        Long id,
        PetitionCategory category,
        PetitionStatus status,
        String title,
        String content,
        int agreementCount,
        int targetAgreementCount,
        boolean hidden,
        String hiddenReason,
        Long hiddenByAdminId,
        LocalDateTime hiddenAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminPetitionResponse from(Petition petition) {
        return new AdminPetitionResponse(petition.getId(), petition.getCategory(),
                petition.getStatus(), petition.getTitle(), petition.getContent(),
                petition.getAgreementCount(), petition.getTargetAgreementCount(),
                petition.isHidden(), petition.getHiddenReason(),
                petition.getHiddenByAdmin() == null ? null : petition.getHiddenByAdmin().getId(),
                petition.getHiddenAt(), petition.getCreatedAt(), petition.getUpdatedAt());
    }
}