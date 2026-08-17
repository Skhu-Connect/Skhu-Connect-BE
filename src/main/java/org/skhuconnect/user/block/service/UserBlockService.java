package org.skhuconnect.user.block.service;

import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.comment.repository.CommentRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.block.dto.UserBlockRequest;
import org.skhuconnect.user.block.dto.UserBlockResponse;
import org.skhuconnect.user.block.entity.BlockTargetType;
import org.skhuconnect.user.block.entity.UserBlock;
import org.skhuconnect.user.block.exception.UserBlockException;
import org.skhuconnect.user.block.repository.UserBlockRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserBlockService {
    private final UserRepository userRepository;
    private final PetitionRepository petitionRepository;
    private final CommentRepository commentRepository;
    private final UserBlockRepository userBlockRepository;

    public UserBlockService(UserRepository userRepository, PetitionRepository petitionRepository,
                            CommentRepository commentRepository, UserBlockRepository userBlockRepository) {
        this.userRepository = userRepository;
        this.petitionRepository = petitionRepository;
        this.commentRepository = commentRepository;
        this.userBlockRepository = userBlockRepository;
    }

    @Transactional
    public UserBlockResponse block(Long blockerId, UserBlockRequest request) {
        User blocker = userRepository.findByIdAndDeletedFalse(blockerId)
                .orElseThrow(() -> error(UserBlockException.Reason.BLOCKER_NOT_FOUND));
        User blockedUser = findWriter(request.targetType(), request.contentId());
        if (blockedUser.isDeleted()) throw error(UserBlockException.Reason.TARGET_USER_NOT_FOUND);
        if (blocker.getId().equals(blockedUser.getId())) throw error(UserBlockException.Reason.SELF_BLOCK);
        if (userBlockRepository.existsByBlockerIdAndBlockedUserId(blockerId, blockedUser.getId())) {
            throw error(UserBlockException.Reason.ALREADY_BLOCKED);
        }
        try {
            UserBlock saved = userBlockRepository.saveAndFlush(UserBlock.create(blocker, blockedUser));
            return new UserBlockResponse(saved.getCreatedAt());
        } catch (DataIntegrityViolationException exception) {
            throw error(UserBlockException.Reason.ALREADY_BLOCKED);
        }
    }

    private User findWriter(BlockTargetType targetType, Long contentId) {
        if (targetType == BlockTargetType.PETITION) {
            Petition petition = petitionRepository.findByIdAndDeletedFalseAndHiddenFalse(contentId)
                    .orElseThrow(() -> error(UserBlockException.Reason.CONTENT_NOT_FOUND));
            return petition.getWriter();
        }
        Comment comment = commentRepository.findVisibleById(contentId)
                .orElseThrow(() -> error(UserBlockException.Reason.CONTENT_NOT_FOUND));
        return comment.getWriter();
    }

    private UserBlockException error(UserBlockException.Reason reason) {
        return new UserBlockException(reason);
    }
}
