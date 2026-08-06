package org.skhuconnect.comment.repository;

import org.skhuconnect.comment.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    long countByCommentId(Long commentId);

    @Query("""
            select commentLike.comment.id as commentId, count(commentLike.id) as likeCount
            from CommentLike commentLike
            where commentLike.comment.id in :commentIds
            group by commentLike.comment.id
            """)
    List<CommentLikeCount> countByCommentIds(
            @Param("commentIds") Collection<Long> commentIds
    );

    @Query("""
            select commentLike.comment.id
            from CommentLike commentLike
            where commentLike.user.id = :userId
              and commentLike.comment.id in :commentIds
            """)
    List<Long> findLikedCommentIds(
            @Param("userId") Long userId,
            @Param("commentIds") Collection<Long> commentIds
    );

    @Transactional
    long deleteAllByCommentPetitionId(Long petitionId);
}
