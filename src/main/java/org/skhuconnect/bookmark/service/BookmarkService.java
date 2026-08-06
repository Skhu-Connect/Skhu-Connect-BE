package org.skhuconnect.bookmark.service;

import org.skhuconnect.bookmark.dto.response.BookmarkPageResponse;
import org.skhuconnect.bookmark.dto.response.BookmarkPetitionResponse;
import org.skhuconnect.bookmark.dto.response.BookmarkResponse;
import org.skhuconnect.bookmark.entity.Bookmark;
import org.skhuconnect.bookmark.exception.BookmarkException;
import org.skhuconnect.bookmark.exception.BookmarkException.Reason;
import org.skhuconnect.bookmark.repository.BookmarkRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class BookmarkService {

    private static final int MAX_PAGE_SIZE = 100;

    private final BookmarkRepository bookmarkRepository;
    private final PetitionRepository petitionRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public BookmarkService(
            BookmarkRepository bookmarkRepository,
            PetitionRepository petitionRepository,
            UserRepository userRepository,
            Clock clock
    ) {
        this.bookmarkRepository = bookmarkRepository;
        this.petitionRepository = petitionRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public BookmarkResponse create(Long userId, Long petitionId) {
        Petition petition = findVisiblePetition(petitionId);
        User user = findUser(userId);
        if (bookmarkRepository.existsByPetitionIdAndUserId(petitionId, userId)) {
            throw new BookmarkException(Reason.BOOKMARK_DUPLICATE);
        }

        try {
            bookmarkRepository.saveAndFlush(Bookmark.create(petition, user));
        } catch (DataIntegrityViolationException exception) {
            throw new BookmarkException(Reason.BOOKMARK_DUPLICATE);
        }
        return BookmarkResponse.created(petitionId);
    }

    @Transactional
    public void cancel(Long userId, Long petitionId) {
        findVisiblePetition(petitionId);
        Bookmark bookmark = bookmarkRepository
                .findByPetitionIdAndUserId(petitionId, userId)
                .orElseThrow(() -> new BookmarkException(Reason.BOOKMARK_NOT_FOUND));
        bookmarkRepository.delete(bookmark);
    }

    @Transactional(readOnly = true)
    public BookmarkPageResponse findMine(Long userId, int page, int size) {
        validatePage(page, size);
        if (!userRepository.existsById(userId)) {
            throw new BookmarkException(Reason.USER_NOT_FOUND);
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        ));
        LocalDateTime now = LocalDateTime.now(clock);
        Page<BookmarkPetitionResponse> result = bookmarkRepository
                .findVisibleByUserId(userId, pageable)
                .map(bookmark -> BookmarkPetitionResponse.from(bookmark, now));
        return BookmarkPageResponse.from(result);
    }

    private Petition findVisiblePetition(Long petitionId) {
        return petitionRepository.findByIdAndDeletedFalseAndHiddenFalse(petitionId)
                .orElseThrow(() -> new BookmarkException(Reason.PETITION_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BookmarkException(Reason.USER_NOT_FOUND));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BookmarkException(Reason.INVALID_PAGE);
        }
    }
}
