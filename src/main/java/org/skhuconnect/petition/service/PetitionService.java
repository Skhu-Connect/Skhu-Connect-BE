package org.skhuconnect.petition.service;

import org.skhuconnect.admin.answer.repository.OfficialAnswerRepository;
import org.skhuconnect.petition.dto.request.PetitionCreateRequest;
import org.skhuconnect.petition.dto.response.OfficialAnswerDetailResponse;
import org.skhuconnect.petition.dto.request.PetitionQueryCondition;
import org.skhuconnect.petition.dto.request.PetitionUpdateRequest;
import org.skhuconnect.petition.dto.response.PetitionPageResponse;
import org.skhuconnect.petition.dto.response.PetitionQueryResponse;
import org.skhuconnect.petition.dto.response.PetitionResponse;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.exception.PetitionException;
import org.skhuconnect.petition.exception.PetitionException.Reason;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.petition.repository.PetitionSpecification;
import org.skhuconnect.threshold.entity.ThresholdSetting;
import org.skhuconnect.threshold.repository.ThresholdSettingRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Service
public class PetitionService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Duration CREATE_COOLDOWN = Duration.ofMinutes(10);
    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "createdAt", "createdAt",
            "agreementCount", "agreementCount",
            "expiresAt", "agreementDeadline"
    );

    private final PetitionRepository petitionRepository;
    private final UserRepository userRepository;
    private final ThresholdSettingRepository thresholdSettingRepository;
    private final OfficialAnswerRepository officialAnswerRepository;
    private final Clock clock;

    public PetitionService(
            PetitionRepository petitionRepository,
            UserRepository userRepository,
            ThresholdSettingRepository thresholdSettingRepository,
            OfficialAnswerRepository officialAnswerRepository,
            Clock clock
    ) {
        this.petitionRepository = petitionRepository;
        this.userRepository = userRepository;
        this.thresholdSettingRepository = thresholdSettingRepository;
        this.officialAnswerRepository = officialAnswerRepository;
        this.clock = clock;
    }

    @Transactional
    public PetitionResponse create(Long userId, PetitionCreateRequest request) {
        User writer = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new PetitionException(Reason.USER_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(clock);
        petitionRepository.findLatestCreatedAtByWriterId(userId)
                .map(createdAt -> createdAt.plus(CREATE_COOLDOWN))
                .filter(now::isBefore)
                .ifPresent(retryAt -> {
                    // 남은 시간은 올림한다 - 내림하면 마지막 1초가 "0초 후 가능"으로 표시된다.
                    long retryAfterSeconds = Duration.between(now, retryAt)
                            .plusSeconds(1).minusNanos(1).toSeconds();
                    throw new PetitionException(
                            Reason.PETITION_CREATE_COOLDOWN, retryAfterSeconds);
                });
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
                now
        );
        return PetitionResponse.from(petitionRepository.save(petition));
    }

    @Transactional(readOnly = true)
    public PetitionPageResponse findAll(PetitionQueryCondition condition) {
        return findAll(null, condition);
    }

    @Transactional(readOnly = true)
    public PetitionPageResponse findAll(Long viewerId, PetitionQueryCondition condition) {
        LocalDateTime now = LocalDateTime.now(clock);
        PageRequest pageRequest = createPageRequest(condition);
        Page<PetitionQueryResponse> result = petitionRepository.findAll(
                        PetitionSpecification.query(condition, now, viewerId),
                        pageRequest
                )
                .map(petition -> PetitionQueryResponse.from(petition, now, null));
        return PetitionPageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public PetitionQueryResponse findDetail(Long petitionId) {
        Petition petition = petitionRepository.findByIdAndDeletedFalseAndHiddenFalse(petitionId)
                .orElseThrow(() -> new PetitionException(Reason.PETITION_NOT_FOUND));
        OfficialAnswerDetailResponse officialAnswer = officialAnswerRepository
                .findByPetitionId(petitionId)
                .map(OfficialAnswerDetailResponse::from)
                .orElse(null);
        return PetitionQueryResponse.from(petition, LocalDateTime.now(clock), officialAnswer);
    }

    @Transactional(readOnly = true)
    public PetitionQueryResponse findDetail(Long viewerId, Long petitionId) {
        Petition petition = petitionRepository.findVisibleByIdForViewer(petitionId, viewerId)
                .orElseThrow(() -> new PetitionException(Reason.PETITION_NOT_FOUND));
        OfficialAnswerDetailResponse officialAnswer = officialAnswerRepository
                .findByPetitionId(petitionId)
                .map(OfficialAnswerDetailResponse::from)
                .orElse(null);
        return PetitionQueryResponse.from(petition, LocalDateTime.now(clock), officialAnswer);
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

    private PageRequest createPageRequest(PetitionQueryCondition condition) {
        if (condition.page() < 0
                || condition.size() <= 0
                || condition.size() > MAX_PAGE_SIZE) {
            throw new PetitionException(Reason.INVALID_PAGE);
        }

        String[] sortParts = condition.sort().split(",", -1);
        if (sortParts.length != 2) {
            throw new PetitionException(Reason.INVALID_SORT);
        }
        String property = SORT_PROPERTIES.get(sortParts[0]);
        if (property == null) {
            throw new PetitionException(Reason.INVALID_SORT);
        }

        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(
                    sortParts[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new PetitionException(Reason.INVALID_SORT);
        }

        Sort sort = Sort.by(
                new Sort.Order(direction, property),
                new Sort.Order(direction, "id")
        );
        return PageRequest.of(condition.page(), condition.size(), sort);
    }

    private Petition findPetition(Long petitionId) {
        return petitionRepository.findByIdAndDeletedFalseForUpdate(petitionId)
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
