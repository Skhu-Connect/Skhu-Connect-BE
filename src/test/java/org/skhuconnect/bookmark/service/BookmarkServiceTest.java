package org.skhuconnect.bookmark.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.bookmark.dto.response.BookmarkPageResponse;
import org.skhuconnect.bookmark.entity.Bookmark;
import org.skhuconnect.bookmark.exception.BookmarkException;
import org.skhuconnect.bookmark.repository.BookmarkRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookmarkServiceTest {

    private BookmarkRepository bookmarkRepository;
    private PetitionRepository petitionRepository;
    private UserRepository userRepository;
    private BookmarkService service;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        bookmarkRepository = mock(BookmarkRepository.class);
        petitionRepository = mock(PetitionRepository.class);
        userRepository = mock(UserRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-06T03:00:00Z"),
                ZoneId.of("Asia/Seoul"));
        now = LocalDateTime.of(2026, 8, 6, 12, 0);
        service = new BookmarkService(
                bookmarkRepository, petitionRepository, userRepository, clock);
    }

    @Test
    void createsBookmarkForVisiblePetition() {
        Petition petition = petition();
        User user = user(1L);
        when(petitionRepository.findByIdAndDeletedFalseAndHiddenFalse(10L))
                .thenReturn(Optional.of(petition));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var response = service.create(1L, 10L);

        assertThat(response.petitionId()).isEqualTo(10L);
        assertThat(response.bookmarked()).isTrue();
        verify(bookmarkRepository).saveAndFlush(any(Bookmark.class));
    }

    @Test
    void duplicateAndConcurrentConstraintViolationReturnDuplicateReason() {
        Petition petition = petition();
        User user = user(1L);
        when(petitionRepository.findByIdAndDeletedFalseAndHiddenFalse(10L))
                .thenReturn(Optional.of(petition));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookmarkRepository.existsByPetitionIdAndUserId(10L, 1L)).thenReturn(true);

        assertReason(() -> service.create(1L, 10L),
                BookmarkException.Reason.BOOKMARK_DUPLICATE);

        when(bookmarkRepository.existsByPetitionIdAndUserId(10L, 1L)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(bookmarkRepository).saveAndFlush(any(Bookmark.class));
        assertReason(() -> service.create(1L, 10L),
                BookmarkException.Reason.BOOKMARK_DUPLICATE);
    }

    @Test
    void hiddenDeletedOrMissingPetitionCannotBeBookmarked() {
        when(petitionRepository.findByIdAndDeletedFalseAndHiddenFalse(10L))
                .thenReturn(Optional.empty());

        assertReason(() -> service.create(1L, 10L),
                BookmarkException.Reason.PETITION_NOT_FOUND);
        verify(userRepository, never()).findById(1L);
    }

    @Test
    void cancelDeletesOwnedBookmarkAndMissingBookmarkReturnsNotFound() {
        Petition petition = petition();
        Bookmark bookmark = Bookmark.create(petition, user(1L));
        when(petitionRepository.findByIdAndDeletedFalseAndHiddenFalse(10L))
                .thenReturn(Optional.of(petition));
        when(bookmarkRepository.findByPetitionIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(bookmark), Optional.empty());

        service.cancel(1L, 10L);
        verify(bookmarkRepository).delete(bookmark);
        assertReason(() -> service.cancel(1L, 10L),
                BookmarkException.Reason.BOOKMARK_NOT_FOUND);
    }

    @Test
    void findMineReturnsOnlyRepositoryResultsAndValidatesPageBoundary() {
        Bookmark bookmark = Bookmark.create(petition(), user(1L));
        ReflectionTestUtils.setField(bookmark, "id", 3L);
        ReflectionTestUtils.setField(bookmark, "createdAt", now.minusHours(1));
        when(userRepository.existsById(1L)).thenReturn(true);
        when(bookmarkRepository.findVisibleByUserId(any(Long.class), any()))
                .thenReturn(new PageImpl<>(List.of(bookmark)));

        BookmarkPageResponse response = service.findMine(1L, 0, 100);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).bookmarkId()).isEqualTo(3L);
        assertReason(() -> service.findMine(1L, -1, 20),
                BookmarkException.Reason.INVALID_PAGE);
        assertReason(() -> service.findMine(1L, 0, 0),
                BookmarkException.Reason.INVALID_PAGE);
        assertReason(() -> service.findMine(1L, 0, 101),
                BookmarkException.Reason.INVALID_PAGE);
    }

    @Test
    void missingUserCannotReadBookmarkList() {
        when(userRepository.existsById(1L)).thenReturn(false);
        assertReason(() -> service.findMine(1L, 0, 20),
                BookmarkException.Reason.USER_NOT_FOUND);
    }

    private Petition petition() {
        Petition petition = Petition.create(user(2L), PetitionCategory.FACILITY,
                "title", "content", 10, now.minusDays(1));
        ReflectionTestUtils.setField(petition, "id", 10L);
        return petition;
    }

    private User user(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private void assertReason(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
            BookmarkException.Reason reason
    ) {
        assertThatThrownBy(callable)
                .isInstanceOf(BookmarkException.class)
                .extracting("reason")
                .isEqualTo(reason);
    }
}
