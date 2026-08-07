package org.skhuconnect.admin.answer.service;

import org.skhuconnect.admin.answer.dto.OfficialAnswerRequest;
import org.skhuconnect.admin.answer.dto.OfficialAnswerResponse;
import org.skhuconnect.admin.answer.entity.OfficialAnswer;
import org.skhuconnect.admin.answer.exception.AdminOfficialAnswerException;
import org.skhuconnect.admin.answer.repository.OfficialAnswerRepository;
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.notificationlog.service.AdminNotificationLogService;
import org.skhuconnect.admin.notificationlog.entity.*;
import org.skhuconnect.admin.repository.AdminRepository;
import org.skhuconnect.notification.service.NotificationEventService;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionStatus;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOfficialAnswerService {

    private final AdminRepository adminRepository;
    private final PetitionRepository petitionRepository;
    private final OfficialAnswerRepository officialAnswerRepository;
    private final NotificationEventService notificationEventService;
    private AdminNotificationLogService notificationLogs;

    public AdminOfficialAnswerService(
            AdminRepository adminRepository,
            PetitionRepository petitionRepository,
            OfficialAnswerRepository officialAnswerRepository,
            NotificationEventService notificationEventService
    ) {
        this.adminRepository = adminRepository;
        this.petitionRepository = petitionRepository;
        this.officialAnswerRepository = officialAnswerRepository;
        this.notificationEventService = notificationEventService;
    }

    @Transactional
    public OfficialAnswerResponse register(Long adminId, Long petitionId, OfficialAnswerRequest request) {
        Admin admin = findAdmin(adminId);
        Petition petition = findVisiblePetitionForUpdate(petitionId);
        if (officialAnswerRepository.findByPetitionId(petitionId).isPresent()) {
            throw error(AdminOfficialAnswerException.Reason.OFFICIAL_ANSWER_ALREADY_EXISTS);
        }
        if (petition.getStatus() != PetitionStatus.UNDER_REVIEW) {
            throw error(AdminOfficialAnswerException.Reason.PETITION_NOT_UNDER_REVIEW);
        }

        petition.answer();
        OfficialAnswer answer = OfficialAnswer.create(
                petition, admin, request.content(), request.answerSource());
        OfficialAnswer savedAnswer = officialAnswerRepository.saveAndFlush(answer);
        notificationEventService.onPetitionAnswered(petition);
        if (notificationLogs != null) notificationLogs.record(NotificationLogType.ANSWER_REGISTERED, admin, NotificationLogTargetType.PETITION, petitionId, "Official answer registered");
        return OfficialAnswerResponse.from(savedAnswer);
    }

    @Transactional
    public OfficialAnswerResponse update(Long adminId, Long petitionId, OfficialAnswerRequest request) {
        Admin admin = findAdmin(adminId);
        Petition petition = findVisiblePetitionForUpdate(petitionId);
        if (petition.getStatus() != PetitionStatus.ANSWERED) {
            throw error(AdminOfficialAnswerException.Reason.PETITION_NOT_ANSWERED);
        }
        OfficialAnswer answer = officialAnswerRepository.findByPetitionId(petitionId)
                .orElseThrow(() -> error(AdminOfficialAnswerException.Reason.OFFICIAL_ANSWER_NOT_FOUND));
        answer.update(admin, request.content(), request.answerSource());
        if (notificationLogs != null) notificationLogs.record(NotificationLogType.ANSWER_UPDATED, admin, NotificationLogTargetType.PETITION, petitionId, "Official answer updated");
        return OfficialAnswerResponse.from(answer);
    }

    @org.springframework.beans.factory.annotation.Autowired
    void setNotificationLogs(AdminNotificationLogService notificationLogs) { this.notificationLogs = notificationLogs; }

    private Admin findAdmin(Long adminId) {
        return adminRepository.findById(adminId)
                .orElseThrow(() -> error(AdminOfficialAnswerException.Reason.ADMIN_NOT_FOUND));
    }

    private Petition findVisiblePetitionForUpdate(Long petitionId) {
        return petitionRepository.findVisibleByIdForUpdate(petitionId)
                .orElseThrow(() -> error(AdminOfficialAnswerException.Reason.PETITION_NOT_FOUND));
    }

    private AdminOfficialAnswerException error(AdminOfficialAnswerException.Reason reason) {
        return new AdminOfficialAnswerException(reason);
    }
}