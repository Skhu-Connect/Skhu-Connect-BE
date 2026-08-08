package org.skhuconnect.petition.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.entity.PetitionStatus;

import java.time.LocalDateTime;

public record PetitionQueryResponse(
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
        @JsonInclude(JsonInclude.Include.NON_NULL)
        OfficialAnswerDetailResponse officialAnswer
) {
    public static PetitionQueryResponse from(
            Petition petition,
            LocalDateTime now,
            OfficialAnswerDetailResponse officialAnswer
    ) {
        return new PetitionQueryResponse(
                petition.getId(),
                petition.getCategory(),
                effectiveStatus(petition, now),
                petition.getTitle(),
                petition.getContent(),
                petition.getAgreementCount(),
                petition.getTargetAgreementCount(),
                petition.getAgreementDeadline(),
                petition.getCreatedAt(),
                petition.getUpdatedAt(),
                officialAnswer
        );
    }

    public static PetitionQueryResponse from(Petition petition, LocalDateTime now) {
        return from(petition, now, null);
    }

    private static PetitionStatus effectiveStatus(Petition petition, LocalDateTime now) {
        if (petition.getStatus() == PetitionStatus.OPEN
                && petition.getAgreementDeadline().isBefore(now)
                && petition.getAgreementCount() < petition.getTargetAgreementCount()) {
            return PetitionStatus.EXPIRED;
        }
        return petition.getStatus();
    }
}