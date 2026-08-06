package org.skhuconnect.bookmark.repository;

import org.skhuconnect.bookmark.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByPetitionIdAndUserId(Long petitionId, Long userId);

    Optional<Bookmark> findByPetitionIdAndUserId(Long petitionId, Long userId);

    @EntityGraph(attributePaths = "petition")
    @Query("""
            select bookmark
            from Bookmark bookmark
            where bookmark.user.id = :userId
              and bookmark.petition.hidden = false
              and bookmark.petition.deleted = false
            """)
    Page<Bookmark> findVisibleByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );
}
