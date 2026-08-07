package org.skhuconnect.admin.content.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.admin.content.dto.AdminContentHideRequest;
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.repository.AdminRepository;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.comment.entity.PetitionAnonymousNumber;
import org.skhuconnect.comment.repository.CommentRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminContentServiceTest {

    private AdminRepository admins;
    private PetitionRepository petitions;
    private CommentRepository comments;
    private AdminContentService service;
    private Admin admin;
    private Petition petition;
    private Comment comment;

    @BeforeEach
    void setUp() {
        admins = mock(AdminRepository.class);
        petitions = mock(PetitionRepository.class);
        comments = mock(CommentRepository.class);
        service = new AdminContentService(admins, petitions, comments,
                Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC));
        admin = mock(Admin.class);
        when(admin.getId()).thenReturn(7L);

        User writer = mock(User.class);
        when(writer.getId()).thenReturn(3L);
        petition = Petition.create(writer, PetitionCategory.FACILITY,
                "title", "content", 10, java.time.LocalDateTime.now());
        ReflectionTestUtils.setField(petition, "id", 10L);
        PetitionAnonymousNumber mapping = PetitionAnonymousNumber.create(petition, writer, 1);
        comment = Comment.create(petition, writer, mapping, "comment");
        ReflectionTestUtils.setField(comment, "id", 20L);
    }

    @Test
    void hideAndRestorePetitionPreserveLastHideMetadata() {
        when(petitions.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(petition));
        when(admins.findById(7L)).thenReturn(Optional.of(admin));

        service.hidePetition(7L, 10L, new AdminContentHideRequest("policy violation"));
        service.restorePetition(10L);

        assertThat(petition.isHidden()).isFalse();
        assertThat(petition.getHiddenReason()).isEqualTo("policy violation");
        assertThat(petition.getHiddenByAdmin()).isSameAs(admin);
        assertThat(petition.getHiddenAt()).isEqualTo(java.time.LocalDateTime.of(2030, 1, 1, 0, 0));
    }

    @Test
    void hideAndRestoreReplyPreserveLastHideMetadata() {
        when(comments.findByIdAndPetitionIdAndDeletedFalse(20L, 10L)).thenReturn(Optional.of(comment));
        when(admins.findById(7L)).thenReturn(Optional.of(admin));

        service.hideComment(7L, 10L, 20L, new AdminContentHideRequest("policy violation"));
        service.restoreComment(10L, 20L);

        assertThat(comment.isHidden()).isFalse();
        assertThat(comment.getHiddenReason()).isEqualTo("policy violation");
        assertThat(comment.getHiddenByAdmin()).isSameAs(admin);
        assertThat(comment.getHiddenAt()).isEqualTo(java.time.LocalDateTime.of(2030, 1, 1, 0, 0));
    }
}