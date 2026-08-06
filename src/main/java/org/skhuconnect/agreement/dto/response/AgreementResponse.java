package org.skhuconnect.agreement.dto.response;

import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionStatus;

public record AgreementResponse(
        Long petitionId,
        int agreementCount,
        PetitionStatus status
) {
    public static AgreementResponse from(Petition petition) {
        return new AgreementResponse(
                petition.getId(),
                petition.getAgreementCount(),
                petition.getStatus()
        );
    }
}