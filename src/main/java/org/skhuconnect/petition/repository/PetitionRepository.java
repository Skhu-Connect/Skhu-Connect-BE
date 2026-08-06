package org.skhuconnect.petition.repository;

import jakarta.persistence.LockModeType;
import org.skhuconnect.petition.entity.Petition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PetitionRepository extends JpaRepository<Petition, Long>,
        JpaSpecificationExecutor<Petition> {

    Optional<Petition> findByIdAndDeletedFalse(Long id);

    Optional<Petition> findByIdAndDeletedFalseAndHiddenFalse(Long id);

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