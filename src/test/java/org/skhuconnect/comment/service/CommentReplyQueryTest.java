package org.skhuconnect.comment.service;

import org.junit.jupiter.api.Test;
import org.skhuconnect.comment.entity.*;
import org.skhuconnect.comment.repository.*;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.entity.User;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommentReplyQueryTest {
    @Test void rootPageContainsOrderedRepliesAndParentIdsWithoutNPlusOne() {
        var creation=mock(CommentCreationTransaction.class); var retry=mock(AnonymousNumberRetryService.class);
        var comments=mock(CommentRepository.class); var likes=mock(CommentLikeRepository.class);
        var petitions=mock(PetitionRepository.class);
        var service=new CommentService(creation,retry,comments,likes,petitions,Clock.systemUTC());
        User writer=mock(User.class); when(writer.getId()).thenReturn(1L);
        Petition petition=mock(Petition.class); when(petition.getId()).thenReturn(10L);
        var mapping=PetitionAnonymousNumber.create(petition,writer,1);
        Comment root=Comment.create(petition,writer,mapping,"root"); id(root,1L,LocalDateTime.of(2026,8,6,10,0));
        Comment first=Comment.create(petition,writer,mapping,root,"first"); id(first,2L,LocalDateTime.of(2026,8,6,10,1));
        Comment second=Comment.create(petition,writer,mapping,root,"second"); id(second,3L,LocalDateTime.of(2026,8,6,10,2));
        when(petitions.findByIdAndDeletedFalseAndHiddenFalse(10L)).thenReturn(Optional.of(petition));
        when(comments.findRootPage(eq(10L),any())).thenReturn(new PageImpl<>(List.of(root)));
        when(comments.findByParentCommentIdInAndDeletedFalseOrderByCreatedAtAscIdAsc(List.of(1L)))
                .thenReturn(List.of(first,second));

        var response=service.findAll(null,10L,0,20);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).singleElement().satisfies(found->{
            assertThat(found.parentCommentId()).isNull();
            assertThat(found.replies()).extracting(r->r.id()).containsExactly(2L,3L);
            assertThat(found.replies()).allSatisfy(r->{
                assertThat(r.parentCommentId()).isEqualTo(1L);
                assertThat(r.replies()).isNull();
            });
        });
        verify(comments,times(1)).findByParentCommentIdInAndDeletedFalseOrderByCreatedAtAscIdAsc(List.of(1L));
    }

    @Test void deletingRootDoesNotDeleteExistingReplyObject() {
        User writer=mock(User.class); when(writer.getId()).thenReturn(1L);
        Petition petition=mock(Petition.class); when(petition.getId()).thenReturn(10L);
        var mapping=PetitionAnonymousNumber.create(petition,writer,1);
        Comment root=Comment.create(petition,writer,mapping,"root");
        Comment reply=Comment.create(petition,writer,mapping,root,"reply");
        root.delete(LocalDateTime.of(2026,8,6,12,0));
        assertThat(root.isDeleted()).isTrue();
        assertThat(reply.isDeleted()).isFalse();
        assertThat(reply.getParentComment()).isSameAs(root);
    }

    private static void id(Comment comment,long id,LocalDateTime time){
        ReflectionTestUtils.setField(comment,"id",id);
        ReflectionTestUtils.setField(comment,"createdAt",time);
        ReflectionTestUtils.setField(comment,"updatedAt",time);
    }
}