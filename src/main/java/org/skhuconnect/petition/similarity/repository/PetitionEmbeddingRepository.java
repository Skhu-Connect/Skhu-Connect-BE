package org.skhuconnect.petition.similarity.repository;

import org.skhuconnect.petition.similarity.entity.PetitionEmbedding;
import org.skhuconnect.petition.similarity.entity.PetitionEmbeddingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PetitionEmbeddingRepository extends JpaRepository<PetitionEmbedding, Long> {

    Optional<PetitionEmbedding> findByPetitionId(Long petitionId);

    @EntityGraph(attributePaths = "petition")
    @Query("""
            select embedding
            from PetitionEmbedding embedding
            join embedding.petition petition
            where embedding.status = :status
              and embedding.modelName = :modelName
              and embedding.dimensions = :dimensions
              and petition.deleted = false
              and petition.hidden = false
            """)
    List<PetitionEmbedding> findPublicReadyCandidates(
            @Param("status") PetitionEmbeddingStatus status,
            @Param("modelName") String modelName,
            @Param("dimensions") int dimensions);
}
