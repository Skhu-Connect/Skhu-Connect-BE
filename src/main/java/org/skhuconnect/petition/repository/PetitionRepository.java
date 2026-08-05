package org.skhuconnect.petition.repository;

import org.skhuconnect.petition.entity.Petition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PetitionRepository extends JpaRepository<Petition, Long> {

    Optional<Petition> findByIdAndDeletedFalse(Long id);
}
