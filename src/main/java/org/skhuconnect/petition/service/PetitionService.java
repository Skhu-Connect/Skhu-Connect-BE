package org.skhuconnect.petition.service;

import org.skhuconnect.petition.dto.request.PetitionCreateRequest;
import org.skhuconnect.petition.dto.request.PetitionUpdateRequest;
import org.skhuconnect.petition.dto.response.PetitionResponse;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.exception.PetitionException;
import org.skhuconnect.petition.exception.PetitionException.Reason;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.threshold.entity.ThresholdSetting;
import org.skhuconnect.threshold.repository.ThresholdSettingRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class PetitionService {

    private final PetitionRepository petitionRepository;
    private final UserRepository userRepository;
    private final ThresholdSettingRepository thresholdSettingRepository;
    private final Clock clock;

    public PetitionService(
            PetitionRepository petitionRepository,
            UserRepository userRepository,
            ThresholdSettingRepository thresholdSettingRepository,
            Clock clock
    ) {
        this.petitionRepository = petitionRepository;
        this.userRepository = userRepository;
        this.thresholdSettingRepository = thresholdSettingRepository;
        this.clock = clock;
    }

    @Transactional
    public PetitionResponse create(Long userId, PetitionCreateRequest request) {
        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new PetitionException(Reason.USER_NOT_FOUND));
        ThresholdSetting setting = thresholdSettingRepository
                .findByCategory(request.category())
                .orElseThrow(() -> new PetitionException(
                        Reason.THRESHOLD_SETTING_NOT_FOUND));
        Petition petition = Petition.create(
                writer,
                request.category(),
                request.title(),
                request.content(),
                setting.calculateTargetAgreementCount(),
                LocalDateTime.now(clock)
        );
        return PetitionResponse.from(petitionRepository.save(petition));
    }

    @Transactional
    public PetitionResponse update(
            Long userId,
            Long petitionId,
            PetitionUpdateRequest request
    ) {
        Petition petition = findPetition(petitionId);
        validateWriter(petition, userId);
        validateEditable(petition);
        petition.update(request.title(), request.content());
        return PetitionResponse.from(petition);
    }

    @Transactional
    public void delete(Long userId, Long petitionId) {
        Petition petition = findPetition(petitionId);
        validateWriter(petition, userId);
        validateEditable(petition);
        petition.delete(LocalDateTime.now(clock));
    }

    private Petition findPetition(Long petitionId) {
        return petitionRepository.findByIdAndDeletedFalse(petitionId)
                .orElseThrow(() -> new PetitionException(Reason.PETITION_NOT_FOUND));
    }

    private void validateWriter(Petition petition, Long userId) {
        if (!petition.isWrittenBy(userId)) {
            throw new PetitionException(Reason.PETITION_FORBIDDEN);
        }
    }

    private void validateEditable(Petition petition) {
        if (!petition.isEditable()) {
            throw new PetitionException(Reason.PETITION_NOT_EDITABLE);
        }
    }
}
