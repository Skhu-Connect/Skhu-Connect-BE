package org.skhuconnect.petition.repository;

import jakarta.persistence.LockModeType;
import org.skhuconnect.petition.entity.Petition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PetitionRepository extends JpaRepository<Petition, Long>,
        JpaSpecificationExecutor<Petition> {

    long countByDeletedFalse();
    long countByStatusAndDeletedFalse(org.skhuconnect.petition.entity.PetitionStatus status);

    Optional<Petition> findByIdAndDeletedFalse(Long id);

    @EntityGraph(attributePaths = "writer")
    Page<Petition> findByDeletedFalse(Pageable pageable);

    Optional<Petition> findByIdAndDeletedFalseAndHiddenFalse(Long id);

    @Query("""
            select petition from Petition petition
            where petition.writer.id = :userId
              and petition.deleted = false
              and petition.hidden = false
            """)
    Page<Petition> findVisibleByWriterId(
            @Param("userId") Long userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select petition
            from Petition petition
            where petition.id = :id
              and petition.deleted = false
              and petition.hidden = false
            """)
    Optional<Petition> findVisibleByIdForUpdate(@Param("id") Long id);
}