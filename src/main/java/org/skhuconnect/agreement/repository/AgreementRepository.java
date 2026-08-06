package org.skhuconnect.agreement.repository;

import org.skhuconnect.agreement.entity.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface AgreementRepository extends JpaRepository<Agreement, Long> {

    boolean existsByPetitionIdAndUserId(Long petitionId, Long userId);

    Optional<Agreement> findByPetitionIdAndUserId(Long petitionId, Long userId);

    long countByPetitionId(Long petitionId);

    List<Agreement> findByPetitionId(Long petitionId);

    @Transactional
    long deleteAllByPetitionId(Long petitionId);
}