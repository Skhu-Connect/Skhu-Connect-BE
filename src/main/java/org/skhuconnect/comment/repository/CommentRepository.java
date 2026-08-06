package org.skhuconnect.comment.repository;

import org.skhuconnect.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = {"anonymousNumber", "writer"})
    Page<Comment> findByPetitionIdAndDeletedFalse(Long petitionId, Pageable pageable);

    @EntityGraph(attributePaths = {"anonymousNumber", "writer"})
    @Query(value = "select c from Comment c where c.petition.id = :petitionId and c.parentComment is null " +
            "and (c.deleted = false or exists (select r.id from Comment r where r.parentComment = c and r.deleted = false))",
            countQuery = "select count(c) from Comment c where c.petition.id = :petitionId and c.parentComment is null " +
                    "and (c.deleted = false or exists (select r.id from Comment r where r.parentComment = c and r.deleted = false))")
    Page<Comment> findRootPage(@Param("petitionId") Long petitionId, Pageable pageable);

    @EntityGraph(attributePaths = {"anonymousNumber", "writer", "parentComment"})
    List<Comment> findByParentCommentIdInAndDeletedFalseOrderByCreatedAtAscIdAsc(List<Long> parentIds);

    @EntityGraph(attributePaths = {"petition", "parentComment"})
    Optional<Comment> findByIdAndPetitionId(Long id, Long petitionId);

    @EntityGraph(attributePaths = {"petition", "writer", "anonymousNumber", "parentComment"})
    Optional<Comment> findByIdAndPetitionIdAndDeletedFalse(Long id, Long petitionId);

    @EntityGraph(attributePaths = {"petition", "anonymousNumber", "parentComment"})
    @Query("""
            select comment from Comment comment
            where comment.writer.id = :userId
              and comment.deleted = false
              and comment.petition.deleted = false
              and comment.petition.hidden = false
            """)
    Page<Comment> findVisibleActivityByWriterId(
            @Param("userId") Long userId, Pageable pageable);

    @Transactional
    long deleteAllByPetitionId(Long petitionId);
}