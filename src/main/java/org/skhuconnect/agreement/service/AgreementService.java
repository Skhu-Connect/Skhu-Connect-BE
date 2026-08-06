package org.skhuconnect.agreement.service;

import org.skhuconnect.agreement.dto.response.AgreementResponse;
import org.skhuconnect.agreement.entity.Agreement;
import org.skhuconnect.agreement.exception.AgreementException;
import org.skhuconnect.agreement.exception.AgreementException.Reason;
import org.skhuconnect.agreement.repository.AgreementRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AgreementService {

    private final AgreementRepository agreementRepository;
    private final PetitionRepository petitionRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public AgreementService(
            AgreementRepository agreementRepository,
            PetitionRepository petitionRepository,
            UserRepository userRepository,
            Clock clock
    ) {
        this.agreementRepository = agreementRepository;
        this.petitionRepository = petitionRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public AgreementResponse agree(Long userId, Long petitionId) {
        Petition petition = findLockedPetition(petitionId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AgreementException(Reason.USER_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(clock);
        validateAgreeable(petition, now);
        if (agreementRepository.existsByPetitionIdAndUserId(petitionId, userId)) {
            throw new AgreementException(Reason.AGREEMENT_DUPLICATE);
        }

        try {
            agreementRepository.saveAndFlush(Agreement.create(petition, user));
        } catch (DataIntegrityViolationException exception) {
            throw new AgreementException(Reason.AGREEMENT_DUPLICATE);
        }
        petition.addAgreement(now);
        return AgreementResponse.from(petition);
    }

    @Transactional
    public void cancel(Long userId, Long petitionId) {
        Petition petition = findLockedPetition(petitionId);
        LocalDateTime now = LocalDateTime.now(clock);
        validateAgreeable(petition, now);
        Agreement agreement = agreementRepository
                .findByPetitionIdAndUserId(petitionId, userId)
                .orElseThrow(() -> new AgreementException(
                        Reason.AGREEMENT_NOT_FOUND));
        agreementRepository.delete(agreement);
        petition.removeAgreement(now);
    }

    private Petition findLockedPetition(Long petitionId) {
        return petitionRepository.findVisibleByIdForUpdate(petitionId)
                .orElseThrow(() -> new AgreementException(Reason.PETITION_NOT_FOUND));
    }

    private void validateAgreeable(Petition petition, LocalDateTime now) {
        if (!petition.isAgreementOpenAt(now)) {
            throw new AgreementException(Reason.PETITION_NOT_AGREEABLE);
        }
    }
}