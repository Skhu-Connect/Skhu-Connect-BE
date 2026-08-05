package org.skhuconnect.petition.repository;

import org.skhuconnect.petition.entity.Petition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PetitionRepository extends JpaRepository<Petition, Long>,
        JpaSpecificationExecutor<Petition> {

    Optional<Petition> findByIdAndDeletedFalse(Long id);

    Optional<Petition> findByIdAndDeletedFalseAndHiddenFalse(Long id);
}