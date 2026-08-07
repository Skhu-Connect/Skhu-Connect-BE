package org.skhuconnect.admin.content.service;

import org.skhuconnect.admin.content.dto.AdminCommentResponse;
import org.skhuconnect.admin.content.dto.AdminContentHideRequest;
import org.skhuconnect.admin.content.dto.AdminPageResponse;
import org.skhuconnect.admin.content.dto.AdminPetitionResponse;
import org.skhuconnect.admin.content.exception.AdminContentException;
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.repository.AdminRepository;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.comment.repository.CommentRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AdminContentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminRepository adminRepository;
    private final PetitionRepository petitionRepository;
    private final CommentRepository commentRepository;
    private final Clock clock;

    public AdminContentService(
            AdminRepository adminRepository,
            PetitionRepository petitionRepository,
            CommentRepository commentRepository,
            Clock clock
    ) {
        this.adminRepository = adminRepository;
        this.petitionRepository = petitionRepository;
        this.commentRepository = commentRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminPageResponse<AdminPetitionResponse> findPetitions(int page, int size) {
        return AdminPageResponse.from(petitionRepository.findByDeletedFalse(pageRequest(page, size)),
                AdminPetitionResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminPageResponse<AdminCommentResponse> findComments(
            Long petitionId, int page, int size) {
        findPetition(petitionId);
        return AdminPageResponse.from(commentRepository
                        .findByPetitionIdAndDeletedFalseOrderByCreatedAtDescIdDesc(
                                petitionId, pageRequest(page, size)),
                AdminCommentResponse::from);
    }

    @Transactional
    public AdminPetitionResponse hidePetition(
            Long adminId, Long petitionId, AdminContentHideRequest request) {
        Petition petition = findPetition(petitionId);
        petition.hide(request.hiddenReason(), findAdmin(adminId), LocalDateTime.now(clock));
        return AdminPetitionResponse.from(petition);
    }

    @Transactional
    public AdminPetitionResponse restorePetition(Long petitionId) {
        Petition petition = findPetition(petitionId);
        petition.restore();
        return AdminPetitionResponse.from(petition);
    }

    @Transactional
    public AdminCommentResponse hideComment(
            Long adminId, Long petitionId, Long commentId, AdminContentHideRequest request) {
        Comment comment = findComment(petitionId, commentId);
        comment.hide(request.hiddenReason(), findAdmin(adminId), LocalDateTime.now(clock));
        return AdminCommentResponse.from(comment);
    }

    @Transactional
    public AdminCommentResponse restoreComment(Long petitionId, Long commentId) {
        Comment comment = findComment(petitionId, commentId);
        comment.restore();
        return AdminCommentResponse.from(comment);
    }

    private Petition findPetition(Long petitionId) {
        return petitionRepository.findByIdAndDeletedFalse(petitionId)
                .orElseThrow(() -> error(AdminContentException.Reason.PETITION_NOT_FOUND));
    }

    private Comment findComment(Long petitionId, Long commentId) {
        return commentRepository.findByIdAndPetitionIdAndDeletedFalse(commentId, petitionId)
                .orElseThrow(() -> error(AdminContentException.Reason.COMMENT_NOT_FOUND));
    }

    private Admin findAdmin(Long adminId) {
        return adminRepository.findById(adminId)
                .orElseThrow(() -> error(AdminContentException.Reason.ADMIN_NOT_FOUND));
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw error(AdminContentException.Reason.INVALID_PAGE);
        }
        return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")));
    }

    private AdminContentException error(AdminContentException.Reason reason) {
        return new AdminContentException(reason);
    }
}