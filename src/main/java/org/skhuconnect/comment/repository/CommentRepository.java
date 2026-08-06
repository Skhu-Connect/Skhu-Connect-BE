package org.skhuconnect.comment.repository;

import org.skhuconnect.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"anonymousNumber", "writer"})
    Page<Comment> findByPetitionIdAndDeletedFalse(Long petitionId, Pageable pageable);

    @EntityGraph(attributePaths = {"petition", "writer", "anonymousNumber"})
    Optional<Comment> findByIdAndPetitionIdAndDeletedFalse(Long id, Long petitionId);

    @Transactional
    long deleteAllByPetitionId(Long petitionId);
}
