package org.skhuconnect.petition.similarity.repository;

import org.skhuconnect.petition.similarity.entity.PetitionSimilaritySearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PetitionSimilaritySearchLogRepository
        extends JpaRepository<PetitionSimilaritySearchLog, Long> {

    long countByUserIdAndCountedTrueAndCreatedAtAfter(
            Long userId, LocalDateTime createdAt);

    Optional<PetitionSimilaritySearchLog>
    findTopByUserIdAndQueryHashAndModelNameAndDimensionsAndCreatedAtAfterOrderByCreatedAtDesc(
            Long userId,
            String queryHash,
            String modelName,
            int dimensions,
            LocalDateTime createdAt);

    @Query("""
            select min(log.createdAt)
            from PetitionSimilaritySearchLog log
            where log.user.id = :userId
              and log.counted = true
              and log.createdAt > :createdAt
            """)
    Optional<LocalDateTime> findOldestCountedCreatedAtAfter(
            @Param("userId") Long userId,
            @Param("createdAt") LocalDateTime createdAt);
}
