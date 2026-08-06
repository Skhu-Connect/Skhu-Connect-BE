package org.skhuconnect.comment.repository;

import org.skhuconnect.comment.entity.PetitionAnonymousNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface PetitionAnonymousNumberRepository
        extends JpaRepository<PetitionAnonymousNumber, Long> {

    Optional<PetitionAnonymousNumber> findByPetitionIdAndUserId(
            Long petitionId,
            Long userId
    );

    @Query("""
            select max(mapping.anonymousNumber)
            from PetitionAnonymousNumber mapping
            where mapping.petition.id = :petitionId
            """)
    Optional<Integer> findMaxAnonymousNumberByPetitionId(
            @Param("petitionId") Long petitionId
    );

    @Transactional
    long deleteAllByPetitionId(Long petitionId);
}
