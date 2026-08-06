package org.skhuconnect.agreement.repository;

import org.skhuconnect.agreement.entity.Agreement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface AgreementRepository extends JpaRepository<Agreement, Long> {

    boolean existsByPetitionIdAndUserId(Long petitionId, Long userId);

    Optional<Agreement> findByPetitionIdAndUserId(Long petitionId, Long userId);

    long countByPetitionId(Long petitionId);

    List<Agreement> findByPetitionId(Long petitionId);

    @EntityGraph(attributePaths = "petition")
    @Query("""
            select agreement from Agreement agreement
            where agreement.user.id = :userId
              and agreement.petition.deleted = false
              and agreement.petition.hidden = false
            """)
    Page<Agreement> findVisibleByUserId(
            @Param("userId") Long userId, Pageable pageable);

    @Transactional
    long deleteAllByPetitionId(Long petitionId);
}