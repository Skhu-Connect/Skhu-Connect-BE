package org.skhuconnect.comment.service;

import org.junit.jupiter.api.*;
import org.skhuconnect.comment.dto.request.CommentCreateRequest;
import org.skhuconnect.comment.entity.*;
import org.skhuconnect.comment.exception.CommentException;
import org.skhuconnect.comment.repository.*;
import org.skhuconnect.petition.entity.*;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommentReplyTest {
    private CommentRepository comments;
    private PetitionAnonymousNumberRepository mappings;
    private PetitionRepository petitions;
    private UserRepository users;
    private CommentCreationTransaction transaction;
    private User user;
    private Petition petition;
    private PetitionAnonymousNumber mapping;

    @BeforeEach void setUp() {
        comments=mock(CommentRepository.class); mappings=mock(PetitionAnonymousNumberRepository.class);
        petitions=mock(PetitionRepository.class); users=mock(UserRepository.class);
        transaction=new CommentCreationTransaction(comments,mappings,petitions,users,
                Clock.fixed(Instant.parse("2026-08-06T03:00:00Z"),ZoneId.of("Asia/Seoul")));
        user=mock(User.class); when(user.getId()).thenReturn(1L);
        petition=Petition.create(user,PetitionCategory.FACILITY,"title","content",10,LocalDateTime.of(2026,8,6,12,0));
        ReflectionTestUtils.setField(petition,"id",10L);
        mapping=PetitionAnonymousNumber.create(petition,user,3);
        when(petitions.findVisibleByIdForUpdate(10L)).thenReturn(Optional.of(petition));
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(mappings.findByPetitionIdAndUserId(10L,1L)).thenReturn(Optional.of(mapping));
        when(comments.saveAndFlush(any(Comment.class))).thenAnswer(i->i.getArgument(0));
    }

    @Test void createsRootAndMultipleRepliesWithSameAnonymousNumber() {
        var root=Comment.create(petition,user,mapping,"root"); ReflectionTestUtils.setField(root,"id",11L);
        when(comments.findByIdAndPetitionId(11L,10L)).thenReturn(Optional.of(root));
        var first=transaction.create(1L,10L,"first",11L);
        var second=transaction.create(1L,10L,"second",11L);
        assertThat(first.parentCommentId()).isEqualTo(11L);
        assertThat(second.parentCommentId()).isEqualTo(11L);
        assertThat(first.anonymousNumber()).isEqualTo(3);
        verify(mappings,never()).findMaxAnonymousNumberByPetitionId(any());
    }

    @Test void rejectsReplyToReplyAndDeletedOrHiddenParent() {
        var root=Comment.create(petition,user,mapping,"root"); ReflectionTestUtils.setField(root,"id",11L);
        var reply=Comment.create(petition,user,mapping,root,"reply"); ReflectionTestUtils.setField(reply,"id",12L);
        when(comments.findByIdAndPetitionId(12L,10L)).thenReturn(Optional.of(reply));
        assertReason(12L,CommentException.Reason.REPLY_DEPTH_EXCEEDED);
        root.delete(LocalDateTime.of(2026,8,6,13,0));
        when(comments.findByIdAndPetitionId(11L,10L)).thenReturn(Optional.of(root));
        assertReason(11L,CommentException.Reason.PARENT_COMMENT_DELETED);
        var hidden=Comment.create(petition,user,mapping,"hidden"); ReflectionTestUtils.setField(hidden,"id",13L); ReflectionTestUtils.setField(hidden,"hidden",true);
        when(comments.findByIdAndPetitionId(13L,10L)).thenReturn(Optional.of(hidden));
        assertReason(13L,CommentException.Reason.PARENT_COMMENT_HIDDEN);
    }

    @Test void rejectsMissingOrOtherPetitionParent() {
        when(comments.findByIdAndPetitionId(99L,10L)).thenReturn(Optional.empty());
        assertReason(99L,CommentException.Reason.PARENT_COMMENT_NOT_FOUND);
    }

    @Test void entityRejectsDifferentPetitionAndSecondDepth() {
        var root=Comment.create(petition,user,mapping,"root");
        var reply=Comment.create(petition,user,mapping,root,"reply");
        assertThatThrownBy(()->Comment.create(petition,user,mapping,reply,"nested"))
                .isInstanceOf(IllegalArgumentException.class);
        Petition other=mock(Petition.class); when(other.getId()).thenReturn(20L);
        assertThatThrownBy(()->Comment.create(other,user,mapping,root,"wrong"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void requestSupportsRootAndReply() {
        assertThat(new CommentCreateRequest("root").parentCommentId()).isNull();
        assertThat(new CommentCreateRequest("reply",11L).parentCommentId()).isEqualTo(11L);
    }

    private void assertReason(long parentId, CommentException.Reason reason) {
        assertThatThrownBy(()->transaction.create(1L,10L,"reply",parentId))
                .isInstanceOf(CommentException.class).extracting("reason").isEqualTo(reason);
    }
}