package org.skhuconnect.user.block.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.comment.repository.CommentRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.block.dto.UserBlockRequest;
import org.skhuconnect.user.block.entity.BlockTargetType;
import org.skhuconnect.user.block.entity.UserBlock;
import org.skhuconnect.user.block.exception.UserBlockException;
import org.skhuconnect.user.block.repository.UserBlockRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserBlockServiceTest {
    private UserRepository users; private PetitionRepository petitions; private CommentRepository comments;
    private UserBlockRepository blocks; private UserBlockService service;

    @BeforeEach void setUp() {
        users = mock(UserRepository.class); petitions = mock(PetitionRepository.class);
        comments = mock(CommentRepository.class); blocks = mock(UserBlockRepository.class);
        service = new UserBlockService(users, petitions, comments, blocks);
    }

    @Test void blocksPetitionWriterWithoutExposingIdentity() {
        User blocker = user(1L, false); User target = user(2L, false); Petition petition = mock(Petition.class);
        when(petition.getWriter()).thenReturn(target); when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(blocker));
        when(petitions.findByIdAndDeletedFalseAndHiddenFalse(10L)).thenReturn(Optional.of(petition));
        when(blocks.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.block(1L, new UserBlockRequest(BlockTargetType.PETITION, 10L));

        verify(blocks).saveAndFlush(any(UserBlock.class));
    }

    @Test void blocksCommentOrReplyWriter() {
        User blocker = user(1L, false); User target = user(2L, false); Comment comment = mock(Comment.class);
        when(comment.getWriter()).thenReturn(target); when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(blocker));
        when(comments.findVisibleById(20L)).thenReturn(Optional.of(comment));
        when(blocks.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.block(1L, new UserBlockRequest(BlockTargetType.COMMENT, 20L));

        verify(blocks).saveAndFlush(any(UserBlock.class));
    }

    @Test void rejectsSelfBlockAndDuplicateBlock() {
        User self = user(1L, false); Petition petition = mock(Petition.class);
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(self));
        when(petitions.findByIdAndDeletedFalseAndHiddenFalse(10L)).thenReturn(Optional.of(petition));
        when(petition.getWriter()).thenReturn(self);
        assertThatThrownBy(() -> service.block(1L, new UserBlockRequest(BlockTargetType.PETITION, 10L)))
                .isInstanceOf(UserBlockException.class)
                .extracting(e -> ((UserBlockException) e).getReason())
                .isEqualTo(UserBlockException.Reason.SELF_BLOCK);

        User target = user(2L, false); when(petition.getWriter()).thenReturn(target);
        when(blocks.existsByBlockerIdAndBlockedUserId(1L, 2L)).thenReturn(true);
        assertThatThrownBy(() -> service.block(1L, new UserBlockRequest(BlockTargetType.PETITION, 10L)))
                .isInstanceOf(UserBlockException.class)
                .extracting(e -> ((UserBlockException) e).getReason())
                .isEqualTo(UserBlockException.Reason.ALREADY_BLOCKED);
    }

    @Test void rejectsHiddenDeletedOrWithdrawnTarget() {
        User blocker = user(1L, false);
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(blocker));
        when(petitions.findByIdAndDeletedFalseAndHiddenFalse(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.block(1L, new UserBlockRequest(BlockTargetType.PETITION, 10L)))
                .isInstanceOf(UserBlockException.class);

        Petition petition = mock(Petition.class); User withdrawnTarget = user(2L, true);
        when(petition.getWriter()).thenReturn(withdrawnTarget);
        when(petitions.findByIdAndDeletedFalseAndHiddenFalse(10L)).thenReturn(Optional.of(petition));
        assertThatThrownBy(() -> service.block(1L, new UserBlockRequest(BlockTargetType.PETITION, 10L)))
                .isInstanceOf(UserBlockException.class)
                .extracting(e -> ((UserBlockException) e).getReason())
                .isEqualTo(UserBlockException.Reason.TARGET_USER_NOT_FOUND);
    }

    private User user(Long id, boolean deleted) { User user = mock(User.class); when(user.getId()).thenReturn(id); when(user.isDeleted()).thenReturn(deleted); return user; }
}
